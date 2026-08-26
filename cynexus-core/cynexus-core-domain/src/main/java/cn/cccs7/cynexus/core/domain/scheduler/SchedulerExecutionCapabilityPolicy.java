package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;

/** Resolves whether a configured scheduler job has a Python data-job owner. */
public final class SchedulerExecutionCapabilityPolicy {

    private SchedulerExecutionCapabilityPolicy() {
    }

    public static boolean supportsDataJob(SchedulerJobBO job) {
        return SchedulerTriggerPolicy.resolveDataJob(job).isPresent();
    }
}
