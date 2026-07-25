package cn.cccs7.cynexus.core.domain.service.scheduler;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerOutcome;
import java.time.OffsetDateTime;

public interface SchedulerTriggerDomainService {

    SchedulerTriggerOutcome trigger(SchedulerTriggerCommand command);

    void synchronizeProgress(SchedulerJobBO job, OffsetDateTime startedAt, DataJobTriggerResultBO result);

    void recordResult(SchedulerJobBO job,
                      OffsetDateTime startedAt,
                      OffsetDateTime endedAt,
                      boolean ok,
                      String status,
                      String error,
                      String triggerType,
                      DataJobTriggerResultBO result,
                      Long existingHistoryId);
}
