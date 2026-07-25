package cn.cccs7.cynexus.core.domain.valueobject.scheduler;

import java.time.OffsetDateTime;

public record SchedulerTriggerCommand(
        String jobCode,
        String targetDate,
        boolean dryRun,
        String triggerType,
        String triggeredBy,
        OffsetDateTime startedAt) {
}
