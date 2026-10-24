package cn.cccs7.cynexus.ai.domain.service.vision;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiVisionTaskRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Owns the queue lifecycle while the application layer performs orchestration. */
public class AiVisionTaskDomainService {
    public static final int MAX_IMAGE_COUNT = 2;
    private static final int MAX_PAYLOAD_CHARS = 32 * 1024 * 1024;
    public static final String QUEUED = "queued";
    public static final String RUNNING = "running";
    public static final String SUCCEEDED = "succeeded";
    public static final String FAILED = "failed";

    private final AiVisionTaskRepository repository;
    private final AiVisionTaskRetentionPolicy retentionPolicy = new AiVisionTaskRetentionPolicy();
    private final Clock clock;

    public AiVisionTaskDomainService(AiVisionTaskRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public AiVisionTaskDomainService(AiVisionTaskRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public AiVisionTaskBO enqueue(String scenarioCode, String userInput, String preferredModelCode,
                                  List<AiVisionTaskBO.Image> images, String tenantKey, String principalKey,
                                  String idempotencyKey) {
        if (scenarioCode == null || scenarioCode.isBlank()) throw new IllegalArgumentException("scenarioCode is required");
        validateImages(images);
        return repository.create(AiVisionTaskBO.builder()
                .scenarioCode(scenarioCode.trim()).userInput(userInput).preferredModelCode(preferredModelCode)
                .idempotencyKey(idempotencyKey)
                .images(List.copyOf(images)).tenantKey(tenantKey).principalKey(principalKey)
                .status(QUEUED).progress(0).attemptCount(0).maxAttempts(2).build());
    }

    public AiVisionTaskBO get(Long id, String tenantKey, String principalKey) {
        if (id == null) throw new IllegalArgumentException("task id is required");
        return repository.find(id, tenantKey, principalKey);
    }

    public List<AiVisionTaskBO> list(int requestedLimit, String tenantKey, String principalKey) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        return repository.listByOwner(limit, tenantKey, principalKey);
    }

    public AiVisionTaskBO retry(Long id, String tenantKey, String principalKey) {
        AiVisionTaskBO task = get(id, tenantKey, principalKey);
        if (task == null) return null;
        if (!isRetryable(task)) {
            throw new IllegalStateException("Vision task cannot be retried; failed image inputs are available for seven days");
        }
        AiVisionTaskBO retry = repository.retryFailed(id, tenantKey, principalKey,
                retentionPolicy.failedInputCutoff(OffsetDateTime.now(clock)));
        if (retry == null) throw new IllegalStateException("Vision task is no longer available for retry");
        return retry;
    }

    public boolean isRetryable(AiVisionTaskBO task) {
        return retentionPolicy.canRetry(task, OffsetDateTime.now(clock));
    }

    public int purgeTerminalInputs() {
        return repository.purgeTerminalInputs(retentionPolicy.failedInputCutoff(OffsetDateTime.now(clock)));
    }

    public AiVisionTaskBO claimNext(String workerId) {
        return repository.claimNext(workerId);
    }

    public void releaseClaim(AiVisionTaskBO task) {
        if (task == null || task.getId() == null || task.getClaimToken() == null || task.getClaimToken().isBlank()) return;
        repository.releaseClaim(task);
    }

    public int recoverStaleTasks(int timeoutMinutes) {
        return repository.recoverStaleTasks(timeoutMinutes);
    }

    public void succeed(AiVisionTaskBO task, String executionNo, String providerCode, String modelCode, String content) {
        task.setExecutionNo(executionNo);
        task.setProviderCode(providerCode);
        task.setModelCode(modelCode);
        task.setContent(content);
        task.setProgress(100);
        task.setStatus(SUCCEEDED);
        task.setImages(List.of());
        task.setInputsAvailable(false);
        repository.markSucceeded(task);
    }

    public void fail(AiVisionTaskBO task, String message) {
        task.setErrorMessage(message == null || message.isBlank() ? "Vision task failed" : message);
        task.setStatus(FAILED);
        task.setProgress(100);
        repository.markFailed(task, task.getErrorMessage());
    }

    private void validateImages(List<AiVisionTaskBO.Image> images) {
        if (images == null || images.isEmpty()) throw new IllegalArgumentException("At least one vision image is required");
        if (images.size() > MAX_IMAGE_COUNT) throw new IllegalArgumentException("Vision task requires one or two images");
        long payload = 0;
        for (AiVisionTaskBO.Image image : images) {
            if (image == null || image.getDataUrl() == null || image.getDataUrl().isBlank()) {
                throw new IllegalArgumentException("Vision image data is required");
            }
            if (image.getDataUrl().length() > MAX_PAYLOAD_CHARS) throw new IllegalArgumentException("Vision image is too large");
            payload += image.getDataUrl().length();
        }
        if (payload > MAX_PAYLOAD_CHARS) throw new IllegalArgumentException("Vision task payload is too large");
    }
}
