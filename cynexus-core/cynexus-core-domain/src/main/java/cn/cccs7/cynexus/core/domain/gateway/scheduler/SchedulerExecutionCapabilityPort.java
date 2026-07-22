package cn.cccs7.cynexus.core.domain.gateway.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;

/** Describes whether a configured scheduler job has an executable owner. */
public interface SchedulerExecutionCapabilityPort {

    boolean supports(SchedulerJobBO job);

    boolean isMarketAutomation(SchedulerJobBO job);
}
