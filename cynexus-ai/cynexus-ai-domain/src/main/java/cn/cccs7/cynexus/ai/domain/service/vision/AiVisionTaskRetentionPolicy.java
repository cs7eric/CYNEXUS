package cn.cccs7.cynexus.ai.domain.service.vision;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import java.time.Duration;
import java.time.OffsetDateTime;

/** Failed inputs support explicit retries for seven days; successful inputs are removed immediately. */
public final class AiVisionTaskRetentionPolicy {
    private static final Duration FAILED_INPUT_RETENTION = Duration.ofDays(7);

    public OffsetDateTime failedInputCutoff(OffsetDateTime now) {
        return now.minus(FAILED_INPUT_RETENTION);
    }

    public boolean canRetry(AiVisionTaskBO task, OffsetDateTime now) {
        return task != null && AiVisionTaskDomainService.FAILED.equals(task.getStatus())
                && !task.isHasRetry() && task.isInputsAvailable() && task.getCompletedAt() != null
                && task.getCompletedAt().isAfter(failedInputCutoff(now));
    }
}
