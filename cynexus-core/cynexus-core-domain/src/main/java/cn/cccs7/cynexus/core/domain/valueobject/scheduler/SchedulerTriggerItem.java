package cn.cccs7.cynexus.core.domain.valueobject.scheduler;

public record SchedulerTriggerItem(
        String jobId,
        Boolean ok,
        String lastStatus,
        String lastRunError,
        String lastMessage) {
}
