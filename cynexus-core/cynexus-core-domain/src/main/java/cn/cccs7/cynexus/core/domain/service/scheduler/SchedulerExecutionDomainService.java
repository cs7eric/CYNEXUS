package cn.cccs7.cynexus.core.domain.service.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerDueJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/** Owns due-job selection, execution guards, state transitions and recovery. */
public interface SchedulerExecutionDomainService {

    List<SchedulerDueJobBO> findDueJobs(OffsetDateTime now);

    boolean acquire(SchedulerDueJobBO dueJob);

    void release(SchedulerDueJobBO dueJob);

    boolean isMarketAutomation(SchedulerDueJobBO dueJob);

    void markRunning(SchedulerDueJobBO dueJob, OffsetDateTime startedAt);

    void markStopped(SchedulerDueJobBO dueJob, OffsetDateTime stoppedAt);

    int refreshMarketAutomation(SchedulerDueJobBO dueJob);

    void recordMarketAutomationResult(SchedulerDueJobBO dueJob,
                                      OffsetDateTime startedAt,
                                      Integer targetsProcessed,
                                      RuntimeException failure);

    int recoverExpiredRunningStates(OffsetDateTime now, Duration maxAge);
}
