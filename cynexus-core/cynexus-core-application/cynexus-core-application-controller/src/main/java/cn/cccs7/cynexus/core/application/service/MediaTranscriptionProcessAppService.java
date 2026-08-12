package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionProcessDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionEnqueueDTO;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionProcessRequest;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionProcessDomainService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaTranscriptionProcessAppService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MediaTranscriptionProcessDomainService domainService;
    private final Executor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaTranscriptionProcessAppService(MediaTranscriptionProcessDomainService domainService,
                                               @Qualifier("mediaTranscriptionProcessExecutor") Executor executor) {
        this.domainService = domainService;
        this.executor = executor;
    }

    public MediaTranscriptionProcessDTO submit(MediaTranscriptionProcessRequest request, String idempotencyKey) {
        MediaTranscriptionProcessBO process = domainService.submit(
                request == null ? null : request.getUrl(),
                idempotencyKey,
                request == null ? null : request.getTitle(),
                request == null ? null : request.getPlatform(),
                request == null ? null : request.getNoteType());
        return toDto(process);
    }

    public MediaTranscriptionEnqueueDTO enqueue(MediaTranscriptionProcessRequest request, String idempotencyKey) {
        MediaTranscriptionProcessBO process = domainService.submit(
                request == null ? null : request.getUrl(),
                idempotencyKey,
                request == null ? null : request.getTitle(),
                request == null ? null : request.getPlatform(),
                request == null ? null : request.getNoteType());
        return MediaTranscriptionEnqueueDTO.builder()
                .accepted(true)
                .message("已入队")
                .processId(String.valueOf(process.getId()))
                .status(process.getStatus())
                .build();
    }

    public List<MediaTranscriptionProcessDTO> list(int limit) {
        return domainService.list(limit).stream().map(this::toDto).toList();
    }

    public MediaTranscriptionProcessDTO get(String processId) {
        return toDto(domainService.get(processId));
    }

    public MediaTranscriptionProcessDTO retry(String processId) {
        return toDto(domainService.retry(processId));
    }

    @Scheduled(
            initialDelayString = "${cynexus.media-transcription.process.initial-delay-ms:5000}",
            fixedDelayString = "${cynexus.media-transcription.process.dispatch-delay-ms:2000}"
    )
    public void dispatchQueuedProcesses() {
        MediaTranscriptionProcessBO process = domainService.claimNext();
        if (process != null) {
            executor.execute(() -> domainService.execute(process));
        }
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
}
