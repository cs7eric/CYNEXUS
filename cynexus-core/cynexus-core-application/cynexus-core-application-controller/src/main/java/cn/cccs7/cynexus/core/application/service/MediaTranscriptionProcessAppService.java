package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryItemDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionProcessDTO;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionProcessRequest;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionLinkResolver;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionDomainPolicy;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionProcessDomainPolicy;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionTaskDomainService;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionProcessRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaTranscriptionProcessAppService {

    private static final Logger log = LoggerFactory.getLogger(MediaTranscriptionProcessAppService.class);
    private static final String OWNER_KEY = "default";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MediaTranscriptionProcessRepository repository;
    private final MediaTranscriptionTaskDomainService taskDomainService;
    private final MediaTranscriptionAiClient aiClient;
    private final MediaTranscriptionLinkResolver linkResolver;
    private final MediaTranscriptionAppService historyAppService;
    private final TaskExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MediaTranscriptionProcessDomainPolicy processPolicy = new MediaTranscriptionProcessDomainPolicy();
    private final MediaTranscriptionDomainPolicy mediaPolicy = new MediaTranscriptionDomainPolicy();

    public MediaTranscriptionProcessAppService(
            MediaTranscriptionProcessRepository repository,
            MediaTranscriptionTaskDomainService taskDomainService,
            MediaTranscriptionAiClient aiClient,
            MediaTranscriptionLinkResolver linkResolver,
            MediaTranscriptionAppService historyAppService,
            @Qualifier("mediaTranscriptionProcessExecutor") TaskExecutor executor) {
        this.repository = repository;
        this.taskDomainService = taskDomainService;
        this.aiClient = aiClient;
        this.linkResolver = linkResolver;
        this.historyAppService = historyAppService;
        this.executor = executor;
    }

    public MediaTranscriptionProcessDTO submit(MediaTranscriptionProcessRequest request, String idempotencyKey) {
        String sourceUrl = processPolicy.normalizeUrl(request == null ? null : request.getUrl());
        String normalizedKey = processPolicy.normalizeOptional(idempotencyKey);
        if (normalizedKey == null) {
            normalizedKey = "shortcut-" + UUID.randomUUID();
        }
        MediaTranscriptionProcessBO existing = repository.findByIdempotencyKey(OWNER_KEY, normalizedKey);
        if (existing != null) {
            return toDto(existing);
        }
        MediaTranscriptionProcessBO process = MediaTranscriptionProcessBO.builder()
                .ownerKey(OWNER_KEY)
                .idempotencyKey(normalizedKey)
                .sourceUrl(sourceUrl)
                .title(processPolicy.normalizeOptional(request == null ? null : request.getTitle()))
                .platform(processPolicy.normalizeOptional(request == null ? null : request.getPlatform()))
                .noteType(processPolicy.normalizeOptional(request == null ? null : request.getNoteType()))
                .status("queued")
                .phase("queued")
                .progress(0)
                .build();
        try {
            Long id = repository.create(process);
            return toDto(repository.findById(OWNER_KEY, id));
        } catch (RuntimeException exception) {
            MediaTranscriptionProcessBO raced = repository.findByIdempotencyKey(OWNER_KEY, normalizedKey);
            if (raced != null) {
                return toDto(raced);
            }
            throw exception;
        }
    }

    public List<MediaTranscriptionProcessDTO> list(int limit) {
        return repository.listActive(OWNER_KEY, limit).stream().map(this::toDto).toList();
    }

    public MediaTranscriptionProcessDTO get(String processId) {
        MediaTranscriptionProcessBO process = require(processId);
        return toDto(process);
    }

    public MediaTranscriptionProcessDTO retry(String processId) {
        MediaTranscriptionProcessBO process = require(processId);
        if (!"failed".equalsIgnoreCase(process.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有失败的自动化任务可以重试");
        }
        process.setStatus("queued");
        process.setPhase("queued");
        process.setProgress(0);
        process.setWorkerTaskId(null);
        process.setDownloadUrl(null);
        process.setHistoryId(null);
        process.setTranscript(null);
        process.setPolished(null);
        process.setSummary(null);
        process.setMetadataJson("{}");
        process.setErrorMessage(null);
        process.setStartedAt(null);
        process.setCompletedAt(null);
        repository.update(process);
        return toDto(repository.findById(OWNER_KEY, process.getId()));
    }

    @Scheduled(
            initialDelayString = "${cynexus.media-transcription.process.initial-delay-ms:5000}",
            fixedDelayString = "${cynexus.media-transcription.process.dispatch-delay-ms:2000}"
    )
    public void dispatchQueuedProcesses() {
        MediaTranscriptionProcessBO process = repository.claimNext();
        if (process != null) {
            executor.execute(() -> run(process));
        }
    }

    private void run(MediaTranscriptionProcessBO process) {
        try {
            MediaTranscriptionTaskBO task = startOrResumeWorker(process);
            if ("error".equalsIgnoreCase(task.getStatus())) {
                throw new IllegalStateException(defaultError(task.getError(), "视频转写失败"));
            }
            if (!"done".equalsIgnoreCase(task.getStatus())) {
                task = waitForWorker(process, task);
            }
            if (!"done".equalsIgnoreCase(task.getStatus())) {
                throw new IllegalStateException(defaultError(task.getError(), "视频转写未完成"));
            }
            String transcript = task.getTranscript() == null ? "" : task.getTranscript().trim();
            if (transcript.isBlank()) {
                throw new IllegalStateException("转写结果为空");
            }

            setProgress(process, "polishing", 64);
            String polished = aiClient.polish(transcript);
            setProgress(process, "summarizing", 78);
            Integer duration = task.getMetadata() == null ? null : task.getMetadata().getDuration();
            String title = processPolicy.normalizeOptional(process.getTitle());
            String platform = processPolicy.normalizeOptional(process.getPlatform());
            String summary = aiClient.summarize(transcript, title, platform, duration);
            setProgress(process, "metadata", 88);
            MediaTranscriptionMetadataBO aiMetadata = aiClient.metadata(polished, summary);
            MediaTranscriptionMetadataBO metadata = mediaPolicy.mergeMetadata(task.getMetadata(), aiMetadata);
            MediaTranscriptionTaskBO enriched = mediaPolicy.copyWithAiResult(task, polished, summary, metadata, null);

            setProgress(process, "persisting", 94);
            MediaTranscriptionHistoryItemDTO history = historyAppService.saveCompletedTask(enriched);
            process.setHistoryId(parseLong(history == null ? null : history.getId()));
            process.setStatus("succeeded");
            process.setPhase("completed");
            process.setProgress(100);
            process.setTranscript(transcript);
            process.setPolished(polished);
            process.setSummary(summary);
            process.setMetadataJson(toJson(metadataToMap(metadata)));
            process.setFileName(task.getFileName());
            process.setErrorMessage(null);
            process.setCompletedAt(OffsetDateTime.now());
            repository.update(process);
        } catch (Exception exception) {
            log.warn("Media transcription process failed: id={}", process.getId(), exception);
            process.setStatus("failed");
            process.setPhase("failed");
            process.setProgress(Math.min(99, processPolicy.progress(process.getProgress())));
            process.setErrorMessage(errorMessage(exception));
            process.setCompletedAt(OffsetDateTime.now());
            repository.update(process);
        }
    }

    private MediaTranscriptionTaskBO startOrResumeWorker(MediaTranscriptionProcessBO process) {
        if (process.getWorkerTaskId() != null && !process.getWorkerTaskId().isBlank()) {
            return taskDomainService.getTask(process.getWorkerTaskId());
        }
        MediaTranscriptionResolvedLinkBO resolved = linkResolver.resolve(process.getSourceUrl());
        String downloadUrl = process.getSourceUrl();
        if (resolved != null) {
            downloadUrl = firstText(resolved.getDownloadVideoUrl(), resolved.getDownloadAudioUrl(), process.getSourceUrl());
            if (process.getTitle() == null || process.getTitle().isBlank()) process.setTitle(resolved.getTitle());
            if (process.getPlatform() == null || process.getPlatform().isBlank()) process.setPlatform(resolved.getPlatform());
            if (process.getNoteType() == null || process.getNoteType().isBlank()) process.setNoteType(resolved.getNoteType());
        }
        process.setDownloadUrl(downloadUrl);
        repository.update(process);
        setProgress(process, "downloading", 5);
        MediaTranscriptionTaskRefBO ref = taskDomainService.remoteParse(MediaTranscriptionRemoteParseRequestBO.builder()
                .downloadUrl(downloadUrl)
                .title(process.getTitle())
                .sourceUrl(process.getSourceUrl())
                .platform(process.getPlatform())
                .noteType(process.getNoteType())
                .audioUrl(resolved == null ? null : resolved.getDownloadAudioUrl())
                .originalUrl(process.getSourceUrl())
                .build());
        if (ref == null || ref.getTaskId() == null || ref.getTaskId().isBlank()) {
            throw new IllegalStateException("视频转写任务创建失败");
        }
        process.setWorkerTaskId(ref.getTaskId());
        process.setFileName(ref.getFileName());
        repository.update(process);
        return taskDomainService.getTask(ref.getTaskId());
    }

    private MediaTranscriptionTaskBO waitForWorker(MediaTranscriptionProcessBO process, MediaTranscriptionTaskBO task) {
        long deadline = System.currentTimeMillis() + Duration.ofHours(6).toMillis();
        MediaTranscriptionTaskBO current = task;
        while (System.currentTimeMillis() < deadline) {
            if ("error".equalsIgnoreCase(current.getStatus()) || "done".equalsIgnoreCase(current.getStatus())) {
                return current;
            }
            updateWorkerProgress(process, current);
            sleep(1000);
            current = taskDomainService.getTask(process.getWorkerTaskId());
        }
        throw new IllegalStateException("视频转写超过 6 小时未完成");
    }

    private void updateWorkerProgress(MediaTranscriptionProcessBO process, MediaTranscriptionTaskBO task) {
        String status = task.getStatus() == null ? "" : task.getStatus().toLowerCase();
        if ("downloading".equals(status)) {
            int download = task.getDownloadProgress() == null ? 0 : processPolicy.progress(task.getDownloadProgress().getProgress());
            setProgress(process, "downloading", 5 + Math.min(30, download * 30 / 100));
        } else if ("transcribing".equals(status)) {
            int intake = task.getIntakeProgress() == null ? 0 : processPolicy.progress(task.getIntakeProgress().getProgress());
            setProgress(process, "transcribing", 35 + Math.min(25, intake * 25 / 100));
        }
    }

    private void setProgress(MediaTranscriptionProcessBO process, String phase, int progress) {
        process.setStatus("running");
        process.setPhase(phase);
        process.setProgress(processPolicy.progress(progress));
        repository.update(process);
    }

    private MediaTranscriptionProcessBO require(String processId) {
        Long id = parseLong(processId);
        if (id == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "processId is invalid");
        }
        MediaTranscriptionProcessBO process = repository.findById(OWNER_KEY, id);
        if (process == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "自动化处理记录不存在");
        }
        return process;
    }

    private MediaTranscriptionProcessDTO toDto(MediaTranscriptionProcessBO process) {
        if (process == null) {
            return null;
        }
        return MediaTranscriptionProcessDTO.builder()
                .id(String.valueOf(process.getId()))
                .accepted(true)
                .sourceUrl(process.getSourceUrl())
                .downloadUrl(process.getDownloadUrl())
                .title(process.getTitle())
                .platform(process.getPlatform())
                .noteType(process.getNoteType())
                .workerTaskId(process.getWorkerTaskId())
                .historyId(process.getHistoryId() == null ? null : String.valueOf(process.getHistoryId()))
                .status(process.getStatus())
                .phase(process.getPhase())
                .progress(process.getProgress())
                .fileName(process.getFileName())
                .transcript(process.getTranscript())
                .polished(process.getPolished())
                .summary(process.getSummary())
                .metadata(parseMap(process.getMetadataJson()))
                .errorMessage(process.getErrorMessage())
                .attemptCount(process.getAttemptCount())
                .startedAt(process.getStartedAt())
                .completedAt(process.getCompletedAt())
                .createdAt(process.getCreatedAt())
                .updatedAt(process.getUpdatedAt())
                .build();
    }

    private Map<String, Object> metadataToMap(MediaTranscriptionMetadataBO metadata) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (metadata == null) {
            return map;
        }
        map.put("title", metadata.getTitle());
        map.put("categories", metadata.getCategories());
        map.put("tags", metadata.getTags());
        map.put("source_url", metadata.getSourceUrl());
        map.put("download_url", metadata.getDownloadUrl());
        map.put("platform", metadata.getPlatform());
        map.put("duration", metadata.getDuration());
        map.put("noteType", metadata.getNoteType());
        map.put("download_audio_url", metadata.getAudioUrl());
        map.put("original_url", metadata.getOriginalUrl());
        return map;
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private String defaultError(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis + ThreadLocalRandom.current().nextLong(0, 250));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("自动化任务被中断", exception);
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstText(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }
}
