package cn.cccs7.cynexus.core.domain.service.scheduler.impl;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.client.MarketAutomationClient;
import cn.cccs7.cynexus.core.domain.entity.SchedulerDueJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.gateway.scheduler.SchedulerExecutionCapabilityPort;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerTimingDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerExecutionDomainService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SchedulerExecutionDomainServiceImpl implements SchedulerExecutionDomainService {

    private final SchedulerDomainService schedulerDomainService;
    private final SchedulerTimingDomainService timingDomainService;
    private final SchedulerExecutionCapabilityPort capabilityPort;
    private final MarketAutomationClient marketAutomationClient;
    private final Set<Long> inFlightJobIds = ConcurrentHashMap.newKeySet();

    public SchedulerExecutionDomainServiceImpl(SchedulerDomainService schedulerDomainService,
                                               SchedulerTimingDomainService timingDomainService,
                                               SchedulerExecutionCapabilityPort capabilityPort,
                                               MarketAutomationClient marketAutomationClient) {
        this.schedulerDomainService = schedulerDomainService;
        this.timingDomainService = timingDomainService;
        this.capabilityPort = capabilityPort;
        this.marketAutomationClient = marketAutomationClient;
    }

    @Override
    public List<SchedulerDueJobBO> findDueJobs(OffsetDateTime now) {
        Map<Long, SchedulerJobStatusBO> statusByJobId = schedulerDomainService.listStatuses().stream()
                .filter(status -> status.getDeletedAt() == null)
                .collect(Collectors.toMap(SchedulerJobStatusBO::getJobId, Function.identity(),
                        SchedulerExecutionDomainServiceImpl::pickLatestStatus));
        return schedulerDomainService.listJobs().stream()
                .filter(job -> job != null && job.getDeletedAt() == null)
                .filter(job -> Boolean.TRUE.equals(job.getEnabled()) && capabilityPort.supports(job))
                .map(job -> new SchedulerDueJobBO(job, statusByJobId.get(job.getId()), now))
                .filter(this::isDue)
                .toList();
    }

    @Override
    public boolean acquire(SchedulerDueJobBO dueJob) {
        return dueJob != null && dueJob.getJob() != null
                && inFlightJobIds.add(dueJob.getJob().getId());
    }

    @Override
    public void release(SchedulerDueJobBO dueJob) {
        if (dueJob != null && dueJob.getJob() != null) {
            inFlightJobIds.remove(dueJob.getJob().getId());
        }
    }

    @Override
    public boolean isMarketAutomation(SchedulerDueJobBO dueJob) {
        return dueJob != null && capabilityPort.isMarketAutomation(dueJob.getJob());
    }

    @Override
    public void markRunning(SchedulerDueJobBO dueJob, OffsetDateTime startedAt) {
        SchedulerJobStatusBO status = requireStatus(dueJob);
        schedulerDomainService.updateStatusRunning(status.getId(), true, startedAt, null);
    }

    @Override
    public void markStopped(SchedulerDueJobBO dueJob, OffsetDateTime stoppedAt) {
        SchedulerJobStatusBO status = dueJob == null || dueJob.getJob() == null
                ? null : schedulerDomainService.findStatusByJobId(dueJob.getJob().getId());
        if (status != null && status.getDeletedAt() == null) {
            schedulerDomainService.updateStatusRunning(status.getId(), false,
                    status.getSchedulerStartedAt(), stoppedAt);
        }
    }

    @Override
    public int refreshMarketAutomation(SchedulerDueJobBO dueJob) {
        if (dueJob == null || !capabilityPort.isMarketAutomation(dueJob.getJob())) {
            throw new IllegalArgumentException("Scheduler job is not market automation");
        }
        return marketAutomationClient.refreshInformationPipeline();
    }

    @Override
    public void recordMarketAutomationResult(SchedulerDueJobBO dueJob,
                                             OffsetDateTime startedAt,
                                             Integer targetsProcessed,
                                             RuntimeException failure) {
        SchedulerJobStatusBO status = requireStatus(dueJob);
        OffsetDateTime endedAt = OffsetDateTime.now(startedAt.getOffset());
        BigDecimal duration = BigDecimal.valueOf(
                Math.max(0L, Duration.between(startedAt, endedAt).toMillis()) / 1000.0);
        boolean success = failure == null;
        String resultStatus = success ? CoreConstants.STATUS_SUCCESS : CoreConstants.STATUS_FAILED;
        String error = success ? null : failure.getMessage();
        String remark = targetsProcessed == null
                ? "market_information_pipeline_refresh"
                : "market_information_pipeline_refresh rows_written=" + targetsProcessed;
        schedulerDomainService.updateStatusRunResult(status.getId(), endedAt, success, resultStatus,
                error, duration, targetsProcessed, status.getProgress(), status.getExtra(), remark);
        schedulerDomainService.createRunHistory(SchedulerJobRunHistoryBO.builder()
                .jobId(dueJob.getJob().getId())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .triggerType("auto")
                .status(resultStatus)
                .errorMessage(error)
                .durationSeconds(duration)
                .remark(remark)
                .resultSummary("{}")
                .build());
    }

    @Override
    public int recoverExpiredRunningStates(OffsetDateTime now, Duration maxAge) {
        if (now == null || maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            return 0;
        }
        OffsetDateTime cutoff = now.minus(maxAge);
        int recovered = 0;
        for (SchedulerJobStatusBO status : schedulerDomainService.listStatuses()) {
            if (status.getDeletedAt() != null || !Boolean.TRUE.equals(status.getRunning())
                    || status.getSchedulerStartedAt() == null
                    || !status.getSchedulerStartedAt().isBefore(cutoff)) {
                continue;
            }
            schedulerDomainService.updateStatusRunning(status.getId(), false,
                    status.getSchedulerStartedAt(), now);
            recovered++;
        }
        return recovered;
    }

    private boolean isDue(SchedulerDueJobBO dueJob) {
        SchedulerJobStatusBO status = dueJob.getStatus();
        return status != null
                && status.getDeletedAt() == null
                && Boolean.TRUE.equals(status.getEnabled())
                && !Boolean.TRUE.equals(status.getRunning())
                && !readSchedule(status).isEmpty()
                && timingDomainService.isDue(readSchedule(status), status.getLastRunAt(), dueJob.getEvaluatedAt());
    }

    private SchedulerJobStatusBO requireStatus(SchedulerDueJobBO dueJob) {
        if (dueJob == null || dueJob.getStatus() == null || dueJob.getStatus().getId() == null) {
            throw new IllegalStateException("Scheduler job status is required for execution");
        }
        return dueJob.getStatus();
    }

    private Map<String, Object> readSchedule(SchedulerJobStatusBO status) {
        return SchedulerScheduleJson.parse(status == null ? null : status.getSchedule());
    }

    private static SchedulerJobStatusBO pickLatestStatus(SchedulerJobStatusBO left, SchedulerJobStatusBO right) {
        OffsetDateTime leftTime = left.getUpdatedAt() == null ? left.getCreatedAt() : left.getUpdatedAt();
        OffsetDateTime rightTime = right.getUpdatedAt() == null ? right.getCreatedAt() : right.getUpdatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }
}
