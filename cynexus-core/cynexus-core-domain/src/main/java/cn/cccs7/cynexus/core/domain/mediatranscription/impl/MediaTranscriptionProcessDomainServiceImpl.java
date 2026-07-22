package cn.cccs7.cynexus.core.domain.mediatranscription.impl;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionLinkResolver;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionResolvedLinkBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionDomainPolicy;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionHistoryDomainService;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionProcessDomainPolicy;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionProcessDomainService;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionTaskDomainService;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionProcessRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionProcessDomainServiceImpl implements MediaTranscriptionProcessDomainService {

    private static final String OWNER_KEY = "default";

    private final MediaTranscriptionProcessRepository repository;
    private final MediaTranscriptionTaskDomainService taskService;
    private final MediaTranscriptionAiClient aiClient;
    private final MediaTranscriptionLinkResolver linkResolver;
    private final MediaTranscriptionHistoryDomainService historyService;
    private final MediaTranscriptionProcessDomainPolicy processPolicy = new MediaTranscriptionProcessDomainPolicy();
    private final MediaTranscriptionDomainPolicy mediaPolicy = new MediaTranscriptionDomainPolicy();

    public MediaTranscriptionProcessDomainServiceImpl(MediaTranscriptionProcessRepository repository,
                                                      MediaTranscriptionTaskDomainService taskService,
                                                      MediaTranscriptionAiClient aiClient,
                                                      MediaTranscriptionLinkResolver linkResolver,
                                                      MediaTranscriptionHistoryDomainService historyService) {
        this.repository = repository;
        this.taskService = taskService;
        this.aiClient = aiClient;
        this.linkResolver = linkResolver;
        this.historyService = historyService;
    }

    @Override
    public MediaTranscriptionProcessBO submit(String sourceUrl, String idempotencyKey, String title,
                                              String platform, String noteType) {
        String normalizedUrl = processPolicy.normalizeUrl(sourceUrl);
        String normalizedKey = processPolicy.normalizeOptional(idempotencyKey);
        if (normalizedKey == null) {
            normalizedKey = "shortcut-" + java.util.UUID.randomUUID();
        }
        MediaTranscriptionProcessBO existing = repository.findByIdempotencyKey(OWNER_KEY, normalizedKey);
        if (existing != null) {
            return existing;
        }
        MediaTranscriptionProcessBO process = MediaTranscriptionProcessBO.builder()
                .ownerKey(OWNER_KEY)
                .idempotencyKey(normalizedKey)
                .sourceUrl(normalizedUrl)
                .title(processPolicy.normalizeOptional(title))
                .platform(processPolicy.normalizeOptional(platform))
                .noteType(processPolicy.normalizeOptional(noteType))
                .status("queued")
                .phase("queued")
                .progress(0)
                .attemptCount(0)
                .build();
        try {
            Long id = repository.create(process);
            return repository.findById(OWNER_KEY, id);
        } catch (RuntimeException exception) {
            MediaTranscriptionProcessBO raced = repository.findByIdempotencyKey(OWNER_KEY, normalizedKey);
            if (raced != null) {
                return raced;
            }
            throw exception;
        }
    }

    @Override
    public List<MediaTranscriptionProcessBO> list(int limit) {
        return repository.listActive(OWNER_KEY, Math.max(1, Math.min(limit, 100)));
    }

    @Override
    public MediaTranscriptionProcessBO get(String processId) {
        return require(processId);
    }

    @Override
    public MediaTranscriptionProcessBO retry(String processId) {
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
        return repository.findById(OWNER_KEY, process.getId());
    }

    @Override
    public MediaTranscriptionProcessBO claimNext() {
        return repository.claimNext();
    }

    @Override
    public void execute(MediaTranscriptionProcessBO process) {
        if (process == null) {
            return;
        }
        try {
            markStarted(process);
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
            String summary = aiClient.summarize(transcript, process.getTitle(), process.getPlatform(), duration);
            setProgress(process, "metadata", 88);
            MediaTranscriptionMetadataBO metadata = mediaPolicy.mergeMetadata(
                    task.getMetadata(), aiClient.metadata(polished, summary));
            setProgress(process, "persisting", 94);
            var enriched = mediaPolicy.copyWithAiResult(task, polished, summary, metadata, null);
            var history = historyService.saveCompletedTask(enriched);
            process.setHistoryId(history == null ? null : history.getId());
            process.setStatus("succeeded");
            process.setPhase("completed");
            process.setProgress(100);
            process.setTranscript(transcript);
            process.setPolished(polished);
            process.setSummary(summary);
            process.setMetadataJson("{}");
            process.setFileName(task.getFileName());
            process.setErrorMessage(null);
            process.setCompletedAt(OffsetDateTime.now());
            repository.update(process);
        } catch (Exception exception) {
            process.setStatus("failed");
            process.setPhase("failed");
            process.setProgress(Math.min(99, processPolicy.progress(process.getProgress())));
            process.setErrorMessage(errorMessage(exception));
            process.setCompletedAt(OffsetDateTime.now());
            repository.update(process);
        }
    }

    private void markStarted(MediaTranscriptionProcessBO process) {
        process.setStatus("running");
        process.setPhase("starting");
        process.setStartedAt(OffsetDateTime.now());
        process.setAttemptCount((process.getAttemptCount() == null ? 0 : process.getAttemptCount()) + 1);
        repository.update(process);
    }

    private MediaTranscriptionTaskBO startOrResumeWorker(MediaTranscriptionProcessBO process) {
        if (process.getWorkerTaskId() != null && !process.getWorkerTaskId().isBlank()) {
            return taskService.getTask(process.getWorkerTaskId());
        }
        MediaTranscriptionResolvedLinkBO resolved = linkResolver.resolve(process.getSourceUrl());
        String downloadUrl = firstText(resolved == null ? null : resolved.getDownloadVideoUrl(),
                resolved == null ? null : resolved.getDownloadAudioUrl(), process.getSourceUrl());
        if (resolved != null) {
            if (process.getTitle() == null || process.getTitle().isBlank()) process.setTitle(resolved.getTitle());
            if (process.getPlatform() == null || process.getPlatform().isBlank()) process.setPlatform(resolved.getPlatform());
            if (process.getNoteType() == null || process.getNoteType().isBlank()) process.setNoteType(resolved.getNoteType());
        }
        process.setDownloadUrl(downloadUrl);
        setProgress(process, "downloading", 5);
        MediaTranscriptionTaskRefBO ref = taskService.remoteParse(MediaTranscriptionRemoteParseRequestBO.builder()
                .downloadUrl(downloadUrl).title(process.getTitle()).sourceUrl(process.getSourceUrl())
                .platform(process.getPlatform()).noteType(process.getNoteType())
                .audioUrl(resolved == null ? null : resolved.getDownloadAudioUrl())
                .originalUrl(process.getSourceUrl()).build());
        if (ref == null || ref.getTaskId() == null || ref.getTaskId().isBlank()) {
            throw new IllegalStateException("视频转写任务创建失败");
        }
        process.setWorkerTaskId(ref.getTaskId());
        process.setFileName(ref.getFileName());
        repository.update(process);
        return taskService.getTask(ref.getTaskId());
    }

    private MediaTranscriptionTaskBO waitForWorker(MediaTranscriptionProcessBO process,
                                                    MediaTranscriptionTaskBO task) {
        long deadline = System.currentTimeMillis() + Duration.ofHours(6).toMillis();
        MediaTranscriptionTaskBO current = task;
        while (System.currentTimeMillis() < deadline) {
            if ("error".equalsIgnoreCase(current.getStatus()) || "done".equalsIgnoreCase(current.getStatus())) {
                return current;
            }
            updateWorkerProgress(process, current);
            sleep(1000);
            current = taskService.getTask(process.getWorkerTaskId());
        }
        throw new IllegalStateException("视频转写超过 6 小时未完成");
    }

    private void updateWorkerProgress(MediaTranscriptionProcessBO process, MediaTranscriptionTaskBO task) {
        String status = task.getStatus() == null ? "" : task.getStatus().toLowerCase();
        if ("downloading".equals(status)) {
            int progress = task.getDownloadProgress() == null ? 0
                    : processPolicy.progress(task.getDownloadProgress().getProgress());
            setProgress(process, "downloading", 5 + Math.min(30, progress * 30 / 100));
        } else if ("transcribing".equals(status)) {
            int progress = task.getIntakeProgress() == null ? 0
                    : processPolicy.progress(task.getIntakeProgress().getProgress());
            setProgress(process, "transcribing", 35 + Math.min(25, progress * 25 / 100));
        }
    }

    private void setProgress(MediaTranscriptionProcessBO process, String phase, int progress) {
        process.setStatus("running");
        process.setPhase(phase);
        process.setProgress(processPolicy.progress(progress));
        repository.update(process);
    }

    private MediaTranscriptionProcessBO require(String processId) {
        Long id;
        try {
            id = processId == null || processId.isBlank() ? null : Long.valueOf(processId);
        } catch (NumberFormatException exception) {
            id = null;
        }
        if (id == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "processId is invalid");
        }
        MediaTranscriptionProcessBO process = repository.findById(OWNER_KEY, id);
        if (process == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "自动化处理记录不存在");
        }
        return process;
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
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("自动化任务被中断", exception);
        }
    }

    private String firstText(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }
}
