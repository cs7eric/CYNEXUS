package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskEnqueueDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskSummaryDTO;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionImageRequest;
import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import cn.cccs7.cynexus.ai.domain.service.vision.AiVisionTaskDomainService;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiVisionTaskAppService {
    private final AiVisionTaskDomainService domainService;
    private final ScenarioExecutionAppService scenarioExecutionAppService;
    private final Executor executor;

    public AiVisionTaskAppService(AiVisionTaskDomainService domainService,
                                  ScenarioExecutionAppService scenarioExecutionAppService,
                                  @Qualifier("aiVisionTaskExecutor") Executor executor) {
        this.domainService = domainService;
        this.scenarioExecutionAppService = scenarioExecutionAppService;
        this.executor = executor;
    }

    public AiVisionTaskEnqueueDTO enqueue(AiVisionExecuteRequest request, String tenantKey, String principalKey,
                                          String idempotencyKey) {
        AiVisionExecuteRequest safe = request == null ? new AiVisionExecuteRequest() : request;
        List<AiVisionTaskBO.Image> images = safe.getImages() == null ? List.of() : safe.getImages().stream()
                .map(this::toImage)
                .toList();
        AiVisionTaskBO task = domainService.enqueue(safe.getScenarioCode(), safe.getUserInput(), safe.getPreferredModelCode(),
                images, tenantKey, principalKey, normalizeIdempotencyKey(idempotencyKey));
        String id = String.valueOf(task.getId());
        return AiVisionTaskEnqueueDTO.builder()
                .accepted(true)
                .message("queued")
                .processId(id)
                .status(task.getStatus())
                .workflowType("AI_VISION")
                .workflowKey("ai.vision." + safe.getScenarioCode())
                .pollUrl("/api/ai/executions/vision/tasks/" + id)
                .build();
    }

    public AiVisionTaskDTO get(Long id, String tenantKey, String principalKey) {
        AiVisionTaskBO task = domainService.get(id, tenantKey, principalKey);
        if (task == null) return null;
        return toDto(task);
    }

    public List<AiVisionTaskSummaryDTO> list(int limit, String tenantKey, String principalKey) {
        return domainService.list(limit, tenantKey, principalKey).stream().map(this::toSummaryDto).toList();
    }

    public AiVisionTaskSummaryDTO retry(Long id, String tenantKey, String principalKey) {
        AiVisionTaskBO task = domainService.retry(id, tenantKey, principalKey);
        return task == null ? null : toSummaryDto(task);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    /** Claims one durable queue item and hands the model call to the bounded worker pool. */
    public void dispatchNext() {
        AiVisionTaskBO task = domainService.claimNext("ai-vision-worker");
        if (task == null) return;
        try {
            executor.execute(() -> execute(task));
        } catch (RejectedExecutionException exception) {
            domainService.releaseClaim(task);
        }
    }

    void execute(AiVisionTaskBO task) {
        try {
            AiVisionExecuteRequest request = new AiVisionExecuteRequest();
            request.setScenarioCode(task.getScenarioCode());
            request.setUserInput(task.getUserInput());
            request.setPreferredModelCode(task.getPreferredModelCode());
            request.setImages(task.getImages() == null ? List.of() : task.getImages().stream().map(this::toRequestImage).toList());
            AiExecuteDTO result = scenarioExecutionAppService.executeVision(request, task.getTenantKey(), task.getPrincipalKey());
            domainService.succeed(task, result.getExecutionNo(), result.getProviderCode(), result.getModelCode(), result.getContent());
        } catch (RuntimeException exception) {
            domainService.fail(task, rootMessage(exception));
        }
    }

    public int recoverStaleTasks(int timeoutMinutes) {
        return domainService.recoverStaleTasks(timeoutMinutes);
    }

    public int purgeTerminalInputs() {
        return domainService.purgeTerminalInputs();
    }

    private AiVisionTaskBO.Image toImage(AiVisionImageRequest source) {
        if (source == null) return AiVisionTaskBO.Image.builder().build();
        return AiVisionTaskBO.Image.builder().fileName(source.getFileName()).contentType(source.getContentType()).dataUrl(source.getDataUrl()).build();
    }

    private AiVisionImageRequest toRequestImage(AiVisionTaskBO.Image source) {
        AiVisionImageRequest result = new AiVisionImageRequest();
        result.setFileName(source.getFileName());
        result.setContentType(source.getContentType());
        result.setDataUrl(source.getDataUrl());
        return result;
    }

    private AiVisionTaskDTO toDto(AiVisionTaskBO task) {
        return AiVisionTaskDTO.builder()
                .id(String.valueOf(task.getId())).accepted(true).scenarioCode(task.getScenarioCode())
                .status(task.getStatus()).progress(task.getProgress()).executionNo(task.getExecutionNo())
                .providerCode(task.getProviderCode()).modelCode(task.getModelCode()).content(task.getContent())
                .errorMessage(task.getErrorMessage()).error(task.getErrorMessage()).startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt()).createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
    }

    private AiVisionTaskSummaryDTO toSummaryDto(AiVisionTaskBO task) {
        return AiVisionTaskSummaryDTO.builder().id(String.valueOf(task.getId())).scenarioCode(task.getScenarioCode())
                .idempotencyKey(task.getIdempotencyKey())
                .status(task.getStatus()).progress(task.getProgress()).attemptCount(task.getAttemptCount())
                .maxAttempts(task.getMaxAttempts()).retryable(domainService.isRetryable(task))
                .errorMessage(task.getErrorMessage()).startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt()).createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        Throwable last = failure;
        while (current != null) { last = current; current = current.getCause(); }
        return last == null || last.getMessage() == null || last.getMessage().isBlank() ? "Vision task failed" : last.getMessage();
    }
}
