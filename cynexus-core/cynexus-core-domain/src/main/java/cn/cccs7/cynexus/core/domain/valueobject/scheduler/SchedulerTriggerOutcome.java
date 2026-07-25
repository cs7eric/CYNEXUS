package cn.cccs7.cynexus.core.domain.valueobject.scheduler;

import java.util.List;

public record SchedulerTriggerOutcome(
        boolean ok,
        String jobCode,
        Boolean accepted,
        String runId,
        String status,
        int count,
        int failedCount,
        List<SchedulerTriggerItem> items,
        String error) {
}
