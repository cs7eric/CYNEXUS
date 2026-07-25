package cn.cccs7.cynexus.core.domain.service.scheduler.impl;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDataJobKeyResolver;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerManagementPolicy;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerRunResultDomainPolicy;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerTriggerPolicy;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerTriggerDomainService;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerTriggerDomainServiceImpl implements SchedulerTriggerDomainService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTriggerDomainServiceImpl.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    private final SchedulerDomainService schedulerDomainService;
    private final SchedulerManagementDomainService schedulerManagementDomainService;
    private final SchedulerRunResultDomainPolicy runResultPolicy;
    private final ObjectMapper objectMapper;

    public SchedulerTriggerDomainServiceImpl(SchedulerDomainService schedulerDomainService,
                                             SchedulerManagementDomainService schedulerManagementDomainService) {
        this.schedulerDomainService = schedulerDomainService;
        this.schedulerManagementDomainService = schedulerManagementDomainService;
        this.runResultPolicy = new SchedulerRunResultDomainPolicy();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SchedulerTriggerOutcome trigger(SchedulerTriggerCommand command) {
        SchedulerJobBO job = schedulerManagementDomainService.requireJobByCode(command.jobCode());
        OffsetDateTime startedAt = command.startedAt() == null
                ? OffsetDateTime.now(CHINA_ZONE) : command.startedAt();
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerTriggerPolicy.resolveDataJob(job).orElse(null);
        if (dataJob == null) {
            String error = "No Python data job mapping for scheduler job: " + job.getCode();
            recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false,
                    "failed", error, command.triggerType(), null, null);
            return SchedulerTriggerPolicy.failed(job.getCode(), error);
        }

        Map<String, Object> params = SchedulerTriggerPolicy.triggerParams(dataJob, command.targetDate());
        if (SchedulerTriggerPolicy.requiresProgressPolling(job.getCode())) {
            return triggerProgressJobAsync(job, dataJob, params, command, startedAt);
        }
        try {
            DataJobTriggerResultBO result = schedulerDomainService.triggerDataJob(dataJob.jobKey(),
                    DataJobTriggerRequestBO.builder()
                            .triggeredBy(command.triggeredBy())
                            .dryRun(command.dryRun())
                            .params(params)
                            .build());
            SchedulerTriggerOutcome outcome = SchedulerTriggerPolicy.completed(job.getCode(), result);
            recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), outcome.ok(), outcome.status(),
                    outcome.error(), command.triggerType(), result, null);
            return outcome;
        } catch (RuntimeException exception) {
            recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false,
                    "failed", exception.getMessage(), command.triggerType(), null, null);
            return SchedulerTriggerPolicy.failed(job.getCode(), exception.getMessage());
        }
    }

    @Override
    public void synchronizeProgress(SchedulerJobBO job, OffsetDateTime startedAt, DataJobTriggerResultBO result) {
        SchedulerJobStatusBO jobStatus = schedulerManagementDomainService.ensureStatus(job);
        String status = SchedulerManagementPolicy.normalizeTriggerStatus(result);
        Map<String, Object> extra = runResultPolicy
                .build(jobStatus, result, false, status, null, OffsetDateTime.now(CHINA_ZONE)).extra();
        extra.put("lastRunOk", null);
        extra.put("lastRunError", null);
        extra.putIfAbsent("plannedSteps", SchedulerTriggerPolicy.defaultProgressStepCount());
        Map<String, Object> progress = runResultPolicy.progress(extra, status, OffsetDateTime.now(CHINA_ZONE));
        schedulerDomainService.updateStatusProgress(
                jobStatus.getId(), true, startedAt, status,
                toJson(progress), toJson(extra), asString(progress.get("message")));
    }

    @Override
    public void recordResult(SchedulerJobBO job,
                             OffsetDateTime startedAt,
                             OffsetDateTime endedAt,
                             boolean ok,
                             String status,
                             String error,
                             String triggerType,
                             DataJobTriggerResultBO result,
                             Long existingHistoryId) {
        BigDecimal durationSeconds = runResultPolicy.durationSeconds(startedAt, endedAt);
        SchedulerJobStatusBO jobStatus = schedulerManagementDomainService.ensureStatus(job);
        SchedulerRunResultDomainPolicy.RunFacts facts = runResultPolicy.build(
                jobStatus, result, ok, status, error, endedAt);
        schedulerDomainService.updateStatusRunResult(
                jobStatus.getId(), endedAt, ok, status, error, durationSeconds,
                facts.targetsProcessed(), toJson(facts.progress()), toJson(facts.extra()), facts.remark());
        SchedulerJobRunHistoryBO history = SchedulerJobRunHistoryBO.builder()
                .id(existingHistoryId)
                .jobId(job.getId())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .triggerType(triggerType)
                .status(status)
                .errorMessage(error)
                .durationSeconds(durationSeconds)
                .remark(facts.remark())
                .resultSummary(toJson(facts.resultSummary()))
                .build();
        if (existingHistoryId == null) {
            schedulerDomainService.createRunHistory(history);
        } else {
            schedulerDomainService.updateRunHistory(history);
        }
    }

    private SchedulerTriggerOutcome triggerProgressJobAsync(
            SchedulerJobBO job,
            SchedulerDataJobKeyResolver.ResolvedDataJob dataJob,
            Map<String, Object> params,
            SchedulerTriggerCommand command,
            OffsetDateTime startedAt) {
        SchedulerJobStatusBO jobStatus = schedulerManagementDomainService.ensureStatus(job);
        Map<String, Object> extra = SchedulerTriggerPolicy.startingExtra(job, dataJob.jobKey(), jobStatus.getExtra());
        Map<String, Object> progress = runResultPolicy.progress(extra, "running", startedAt);
        schedulerDomainService.updateStatusProgress(
                jobStatus.getId(), true, startedAt, "running",
                toJson(progress), toJson(extra), job.getCode() + " started");

        Long historyId = createProcessingHistory(job, command.triggerType(), startedAt, extra);
        CompletableFuture.runAsync(() -> runProgressJob(
                job, dataJob, params, command, startedAt, historyId));
        return SchedulerTriggerPolicy.processing(job.getCode());
    }

    private Long createProcessingHistory(SchedulerJobBO job,
                                         String triggerType,
                                         OffsetDateTime startedAt,
                                         Map<String, Object> extra) {
        try {
            return schedulerDomainService.createRunHistory(SchedulerJobRunHistoryBO.builder()
                    .jobId(job.getId())
                    .startedAt(startedAt)
                    .endedAt(startedAt)
                    .triggerType(triggerType)
                    .status("processing")
                    .durationSeconds(BigDecimal.ZERO)
                    .remark(job.getCode() + " processing")
                    .resultSummary(toJson(runResultPolicy.resultSummary(
                            null, extra, "processing", null, startedAt)))
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Create scheduler processing history failed, continue triggering data job: jobCode={}",
                    job.getCode(), exception);
            return null;
        }
    }

    private void runProgressJob(SchedulerJobBO job,
                                SchedulerDataJobKeyResolver.ResolvedDataJob dataJob,
                                Map<String, Object> params,
                                SchedulerTriggerCommand command,
                                OffsetDateTime startedAt,
                                Long historyId) {
        try {
            DataJobTriggerResultBO result = schedulerDomainService.triggerDataJob(dataJob.jobKey(),
                    DataJobTriggerRequestBO.builder()
                            .triggerRequestId(job.getCode() + "-" + command.triggerType() + "-" + UUID.randomUUID())
                            .triggeredBy(command.triggeredBy())
                            .dryRun(command.dryRun())
                            .params(params)
                            .build());
            if (SchedulerManagementPolicy.shouldPoll(result)) {
                pollProgressDataJobRun(job, startedAt, command.triggerType(), result, historyId);
                return;
            }
            SchedulerTriggerOutcome outcome = SchedulerTriggerPolicy.completed(job.getCode(), result);
            recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), outcome.ok(), outcome.status(),
                    outcome.error(), command.triggerType(), result, historyId);
        } catch (RuntimeException exception) {
            recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false,
                    "failed", exception.getMessage(), command.triggerType(), null, historyId);
            log.warn("Async scheduler chain trigger failed: jobCode={}", job.getCode(), exception);
        }
    }

    private void pollProgressDataJobRun(SchedulerJobBO job,
                                        OffsetDateTime startedAt,
                                        String triggerType,
                                        DataJobTriggerResultBO initialResult,
                                        Long historyId) {
        String runId = initialResult.getRunId();
        String jobKey = initialResult.getJobKey();
        long deadlineMs = System.currentTimeMillis() + Duration.ofHours(6).toMillis();
        synchronizeProgress(job, startedAt, initialResult);
        while (System.currentTimeMillis() < deadlineMs) {
            try {
                Thread.sleep(2000L);
                Map<String, Object> run = schedulerDomainService.getDataJobRun(runId);
                DataJobTriggerResultBO snapshot = DataJobTriggerResultBO.builder()
                        .accepted(true)
                        .runId(runId)
                        .jobKey(jobKey)
                        .status(asString(run.get("status")))
                        .result(run)
                        .build();
                String status = SchedulerManagementPolicy.normalizeTriggerStatus(snapshot);
                if (SchedulerManagementPolicy.isTerminalStatus(status)) {
                    boolean ok = SchedulerManagementPolicy.isAcceptedSuccess(snapshot);
                    String error = SchedulerManagementPolicy.triggerError(snapshot);
                    recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), ok,
                            status, error, triggerType, snapshot, historyId);
                    return;
                }
                synchronizeProgress(job, startedAt, snapshot);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false,
                        "failed", "poll interrupted", triggerType, initialResult, historyId);
                return;
            } catch (RuntimeException exception) {
                log.warn("Polling chain data job run failed: runId={}", runId, exception);
            }
        }
        recordResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false,
                "failed", "poll timeout", triggerType, initialResult, historyId);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
