package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.SchedulerJobCategoryDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobCategoryMappingDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobRunHistoryDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobStatusDTOConverter;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryMappingDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobRunHistoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobStatusDTO;
import cn.cccs7.cynexus.core.application.dto.DataJobTriggerResultDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementCategoriesResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementDailyStatsResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobActionResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobHistoryResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobsResponseDTO;
import cn.cccs7.cynexus.core.application.request.DataJobTriggerRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementTriggerRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerJobCreateRequest;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.service.SchedulerDomainService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchedulerAppService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerAppService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String MARKET_SENTIMENT_CHAIN_JOB_CODE = "market_sentiment_chain_refresh";
    private static final int MARKET_SENTIMENT_CHAIN_STEP_COUNT = 14;

    private final SchedulerDomainService schedulerDomainService;
    private final ObjectMapper objectMapper;

    public SchedulerAppService(SchedulerDomainService schedulerDomainService) {
        this.schedulerDomainService = schedulerDomainService;
        this.objectMapper = new ObjectMapper();
    }

    public List<SchedulerJobDTO> listJobs() {
        return SchedulerJobDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listJobs());
    }

    public SchedulerJobDTO findJobById(Long id) {
        SchedulerJobBO job = schedulerDomainService.findJobById(id);
        return job == null ? null : SchedulerJobDTOConverter.INSTANCE.toSource(job);
    }

    public Long createJob(SchedulerJobCreateRequest request) {
        SchedulerJobDTO dto = SchedulerJobDTO.builder()
                .id(request.getId())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .serviceModule(request.getServiceModule())
                .serviceClass(request.getServiceClass())
                .configFile(request.getConfigFile())
                .enabled(request.getEnabled())
                .registeredAt(request.getRegisteredAt())
                .extra(request.getExtra())
                .remark(request.getRemark())
                .build();
        return schedulerDomainService.createJob(SchedulerJobDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateJob(SchedulerJobDTO dto) {
        return schedulerDomainService.updateJob(SchedulerJobDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteJob(Long id) {
        return schedulerDomainService.deleteJob(id);
    }

    public List<SchedulerJobCategoryDTO> listCategories() {
        return SchedulerJobCategoryDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listCategories());
    }

    public SchedulerJobCategoryDTO findCategoryById(Long id) {
        SchedulerJobCategoryBO category = schedulerDomainService.findCategoryById(id);
        return category == null ? null : SchedulerJobCategoryDTOConverter.INSTANCE.toSource(category);
    }

    public Long createCategory(SchedulerJobCategoryDTO dto) {
        return schedulerDomainService.createCategory(SchedulerJobCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategory(SchedulerJobCategoryDTO dto) {
        return schedulerDomainService.updateCategory(SchedulerJobCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategory(Long id) {
        return schedulerDomainService.deleteCategory(id);
    }

    public List<SchedulerJobCategoryMappingDTO> listCategoryMappings() {
        return SchedulerJobCategoryMappingDTOConverter.INSTANCE.toSourceList(
                schedulerDomainService.listCategoryMappings());
    }

    public SchedulerJobCategoryMappingDTO findCategoryMappingById(Long id) {
        SchedulerJobCategoryMappingBO mapping = schedulerDomainService.findCategoryMappingById(id);
        return mapping == null ? null : SchedulerJobCategoryMappingDTOConverter.INSTANCE.toSource(mapping);
    }

    public Long createCategoryMapping(SchedulerJobCategoryMappingDTO dto) {
        return schedulerDomainService.createCategoryMapping(
                SchedulerJobCategoryMappingDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategoryMapping(SchedulerJobCategoryMappingDTO dto) {
        return schedulerDomainService.updateCategoryMapping(
                SchedulerJobCategoryMappingDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategoryMapping(Long id) {
        return schedulerDomainService.deleteCategoryMapping(id);
    }

    public List<SchedulerJobRunHistoryDTO> listRunHistory() {
        return SchedulerJobRunHistoryDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listRunHistory());
    }

    public SchedulerJobRunHistoryDTO findRunHistoryById(Long id) {
        SchedulerJobRunHistoryBO runHistory = schedulerDomainService.findRunHistoryById(id);
        return runHistory == null ? null : SchedulerJobRunHistoryDTOConverter.INSTANCE.toSource(runHistory);
    }

    public Long createRunHistory(SchedulerJobRunHistoryDTO dto) {
        return schedulerDomainService.createRunHistory(SchedulerJobRunHistoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateRunHistory(SchedulerJobRunHistoryDTO dto) {
        return schedulerDomainService.updateRunHistory(SchedulerJobRunHistoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteRunHistory(Long id) {
        return schedulerDomainService.deleteRunHistory(id);
    }

    public List<SchedulerJobStatusDTO> listStatuses() {
        return SchedulerJobStatusDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listStatuses());
    }

    public SchedulerJobStatusDTO findStatusById(Long id) {
        SchedulerJobStatusBO status = schedulerDomainService.findStatusById(id);
        return status == null ? null : SchedulerJobStatusDTOConverter.INSTANCE.toSource(status);
    }

    public Long createStatus(SchedulerJobStatusDTO dto) {
        return schedulerDomainService.createStatus(SchedulerJobStatusDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateStatus(SchedulerJobStatusDTO dto) {
        return schedulerDomainService.updateStatus(SchedulerJobStatusDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteStatus(Long id) {
        return schedulerDomainService.deleteStatus(id);
    }

    public Map<String, Object> dataJobExecutorHealth() {
        return schedulerDomainService.dataJobExecutorHealth();
    }

    public Map<String, Object> listDataJobs() {
        return schedulerDomainService.listDataJobs();
    }

    public Map<String, Object> getDataJob(String jobKey) {
        return schedulerDomainService.getDataJob(jobKey);
    }

    public DataJobTriggerResultDTO triggerDataJob(String jobKey, DataJobTriggerRequest request) {
        DataJobTriggerRequestBO bo = DataJobTriggerRequestBO.builder()
                .triggerRequestId(request == null ? null : request.getTriggerRequestId())
                .triggeredBy(request == null ? null : request.getTriggeredBy())
                .dryRun(request != null && Boolean.TRUE.equals(request.getDryRun()))
                .params(request == null || request.getParams() == null ? Map.of() : request.getParams())
                .build();
        DataJobTriggerResultBO result = schedulerDomainService.triggerDataJob(jobKey, bo);
        return DataJobTriggerResultDTO.builder()
                .accepted(result.getAccepted())
                .runId(result.getRunId())
                .jobKey(result.getJobKey())
                .status(result.getStatus())
                .result(result.getResult())
                .build();
    }

    public Map<String, Object> getDataJobRun(String runId) {
        return schedulerDomainService.getDataJobRun(runId);
    }

    public SchedulerManagementJobsResponseDTO listManagementJobs() {
        reconcileRunningDataJobRuns();
        List<SchedulerJobBO> jobs = schedulerDomainService.listJobs().stream()
                .filter(job -> job.getDeletedAt() == null)
                .sorted(Comparator.comparing(SchedulerJobBO::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<Long, SchedulerJobStatusBO> statusByJobId = schedulerDomainService.listStatuses().stream()
                .filter(status -> status.getDeletedAt() == null)
                .collect(Collectors.toMap(SchedulerJobStatusBO::getJobId, Function.identity(), this::pickLatestStatus));
        Map<Long, List<SchedulerJobCategoryMappingBO>> mappingsByJobId = schedulerDomainService.listCategoryMappings()
                .stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .collect(Collectors.groupingBy(SchedulerJobCategoryMappingBO::getJobId));

        List<SchedulerManagementJobsResponseDTO.Item> items = jobs.stream()
                .map(job -> toManagementJobItem(job, statusByJobId.get(job.getId()), mappingsByJobId.get(job.getId())))
                .toList();
        return SchedulerManagementJobsResponseDTO.builder()
                .ok(true)
                .items(items)
                .count(items.size())
                .build();
    }

    public SchedulerManagementCategoriesResponseDTO listManagementCategories() {
        Map<Long, Long> countByCategoryId = schedulerDomainService.listCategoryMappings().stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .collect(Collectors.groupingBy(SchedulerJobCategoryMappingBO::getCategoryId, Collectors.counting()));
        List<SchedulerManagementCategoriesResponseDTO.Item> items = schedulerDomainService.listCategories().stream()
                .filter(category -> category.getDeletedAt() == null)
                .sorted(Comparator.comparing(SchedulerJobCategoryBO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(category -> SchedulerManagementCategoriesResponseDTO.Item.builder()
                        .id(String.valueOf(category.getId()))
                        .label(category.getLabel())
                        .iconHint(category.getIconHint())
                        .sortOrder(category.getSortOrder())
                        .description(category.getDescription())
                        .count(countByCategoryId.getOrDefault(category.getId(), 0L).intValue())
                        .build())
                .toList();
        return SchedulerManagementCategoriesResponseDTO.builder()
                .ok(true)
                .items(items)
                .count(items.size())
                .build();
    }

    public SchedulerManagementDailyStatsResponseDTO listManagementDailyStats(int days) {
        int boundedDays = Math.max(1, Math.min(days, 90));
        LocalDate end = LocalDate.now(CHINA_ZONE);
        LocalDate start = end.minusDays(boundedDays - 1L);
        OffsetDateTime startedAt = start.atStartOfDay(CHINA_ZONE).toOffsetDateTime();
        List<SchedulerJobRunHistoryBO> history = schedulerDomainService.listRunHistorySince(startedAt);
        Map<LocalDate, DayCounter> counters = new LinkedHashMap<>();
        for (int i = 0; i < boundedDays; i++) {
            counters.put(start.plusDays(i), new DayCounter());
        }
        for (SchedulerJobRunHistoryBO item : history) {
            if (item.getStartedAt() == null) {
                continue;
            }
            LocalDate date = item.getStartedAt().atZoneSameInstant(CHINA_ZONE).toLocalDate();
            DayCounter counter = counters.get(date);
            if (counter != null) {
                counter.accept(item.getStatus());
            }
        }
        List<SchedulerManagementDailyStatsResponseDTO.Item> items = counters.entrySet().stream()
                .map(entry -> SchedulerManagementDailyStatsResponseDTO.Item.builder()
                        .date(entry.getKey())
                        .total(entry.getValue().total)
                        .success(entry.getValue().success)
                        .failed(entry.getValue().failed)
                        .skipped(entry.getValue().skipped)
                        .build())
                .toList();
        int total = counters.values().stream().mapToInt(counter -> counter.total).sum();
        int failed = counters.values().stream().mapToInt(counter -> counter.failed).sum();
        double successRate = total == 0 ? 0D : (double) (total - failed) / total;
        return SchedulerManagementDailyStatsResponseDTO.builder()
                .ok(true)
                .items(items)
                .summary(SchedulerManagementDailyStatsResponseDTO.Summary.builder()
                        .total(total)
                        .failed(failed)
                        .successRate(successRate)
                        .build())
                .build();
    }

    public SchedulerManagementJobHistoryResponseDTO listManagementJobHistory(String jobCode, int limit) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        reconcileRunningDataJobRun(job);
        reconcileOpenRunHistories(job);
        int boundedLimit = Math.max(1, Math.min(limit, 200));
        List<SchedulerManagementJobHistoryResponseDTO.Item> items = schedulerDomainService
                .listRunHistoryByJobId(job.getId(), boundedLimit)
                .stream()
                .map(this::toManagementHistoryItem)
                .toList();
        return SchedulerManagementJobHistoryResponseDTO.builder()
                .ok(true)
                .jobId(job.getCode())
                .items(items)
                .count(items.size())
                .build();
    }

    public SchedulerManagementJobActionResponseDTO enableManagementJob(String jobCode) {
        return updateManagementJobEnabled(jobCode, true);
    }

    public SchedulerManagementJobActionResponseDTO disableManagementJob(String jobCode) {
        return updateManagementJobEnabled(jobCode, false);
    }

    public SchedulerManagementJobActionResponseDTO startManagementJob(String jobCode) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        return unsupportedRuntimeAction(job, "start");
    }

    public SchedulerManagementJobActionResponseDTO stopManagementJob(String jobCode) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        return unsupportedRuntimeAction(job, "stop");
    }

    public SchedulerManagementJobActionResponseDTO deleteManagementJob(String jobCode) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        schedulerDomainService.listCategoryMappings().stream()
                .filter(mapping -> Objects.equals(mapping.getJobId(), job.getId()))
                .filter(mapping -> mapping.getDeletedAt() == null)
                .forEach(mapping -> schedulerDomainService.deleteCategoryMapping(mapping.getId()));
        SchedulerJobStatusBO status = schedulerDomainService.findStatusByJobId(job.getId());
        if (status != null) {
            schedulerDomainService.deleteStatus(status.getId());
        }
        boolean deleted = schedulerDomainService.deleteJob(job.getId());
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(deleted)
                .jobId(job.getCode())
                .status(deleted ? "deleted" : "delete_failed")
                .count(deleted ? 1 : 0)
                .failedCount(deleted ? 0 : 1)
                .build();
    }

    public SchedulerManagementJobActionResponseDTO triggerManagementJob(String jobCode,
                                                                        SchedulerManagementTriggerRequest request) {
        return triggerJob(jobCode, request, "manual", "java-scheduler-management");
    }

    public SchedulerManagementJobActionResponseDTO triggerScheduledJob(String jobCode) {
        SchedulerManagementTriggerRequest request = SchedulerManagementTriggerRequest.builder()
                .targetDate(LocalDate.now(CHINA_ZONE).toString())
                .build();
        return triggerJob(jobCode, request, "auto", "java-scheduler-runtime");
    }

    public void reconcileRunningDataJobRuns() {
        Map<Long, SchedulerJobBO> jobsById = schedulerDomainService.listJobs().stream()
                .filter(job -> job.getDeletedAt() == null)
                .collect(Collectors.toMap(SchedulerJobBO::getId, Function.identity(), this::pickLatestJob));
        schedulerDomainService.listStatuses().stream()
                .filter(this::shouldReconcileDataJobStatus)
                .forEach(status -> reconcileRunningDataJobRun(jobsById.get(status.getJobId()), status));
    }

    private void reconcileRunningDataJobRun(SchedulerJobBO job) {
        if (job == null || job.getDeletedAt() != null) {
            return;
        }
        reconcileDataJobRun(job, schedulerDomainService.findStatusByJobId(job.getId()), true);
    }

    private void reconcileRunningDataJobRun(SchedulerJobBO job, SchedulerJobStatusBO status) {
        reconcileDataJobRun(job, status, false);
    }

    private void reconcileDataJobRun(SchedulerJobBO job, SchedulerJobStatusBO status, boolean allowOpenHistoryRepair) {
        if (job == null || status == null) {
            return;
        }
        String runId = extractDataJobRunId(status);
        if (runId == null || runId.isBlank()) {
            return;
        }
        SchedulerJobRunHistoryBO openHistory = findOpenHistory(job.getId(), runId);
        Long historyId = openHistory == null ? null : openHistory.getId();
        if (!shouldReconcileDataJobStatus(status) && !(allowOpenHistoryRepair && historyId != null)) {
            return;
        }
        try {
            Map<String, Object> run = schedulerDomainService.getDataJobRun(runId);
            DataJobTriggerResultBO snapshot = dataJobRunSnapshot(runId, run);
            String finalStatus = normalizeTriggerStatus(snapshot);
            if (!isTerminalDataJobStatus(finalStatus)) {
                syncChainProgress(job, firstNonNull(status.getSchedulerStartedAt(), snapshotStartedAt(run, status)), snapshot);
                return;
            }
            boolean ok = isTriggerAcceptedSuccess(snapshot);
            String error = triggerError(snapshot);
            OffsetDateTime endedAt = snapshotEndedAt(run);
            OffsetDateTime startedAt = safeStartedAt(snapshotStartedAt(run, status), status.getSchedulerStartedAt(), endedAt);
            String triggerType = openHistory == null ? "manual" : allowedHistoryTriggerType(openHistory.getTriggerType());
            recordTriggerResult(job, startedAt, endedAt, ok, finalStatus, error, triggerType, snapshot, historyId);
        } catch (RuntimeException exception) {
            log.warn("Reconcile scheduler data job run failed: jobCode={}, runId={}", job.getCode(), runId, exception);
        }
    }

    private SchedulerManagementJobActionResponseDTO triggerJob(String jobCode,
                                                               SchedulerManagementTriggerRequest request,
                                                               String triggerType,
                                                               String triggeredBy) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        OffsetDateTime startedAt = OffsetDateTime.now(CHINA_ZONE);
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerDataJobKeyResolver.resolve(job.getCode()).orElse(null);
        if (dataJob == null) {
            String error = "No Python data job mapping for scheduler job: " + job.getCode();
            recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false, "failed", error, triggerType);
            return SchedulerManagementJobActionResponseDTO.builder()
                    .ok(false)
                    .jobId(job.getCode())
                    .accepted(false)
                    .status("failed")
                    .count(0)
                    .failedCount(1)
                    .items(List.of())
                    .error(error)
                    .build();
        }
        Map<String, Object> params = new LinkedHashMap<>(dataJob.defaultParams());
        if (request != null && request.getTargetDate() != null && !request.getTargetDate().isBlank()) {
            params.put("target_date", request.getTargetDate());
            params.putIfAbsent("trade_date", request.getTargetDate());
            params.putIfAbsent("snapshot_date", request.getTargetDate());
        }
        if (MARKET_SENTIMENT_CHAIN_JOB_CODE.equals(job.getCode())) {
            return triggerChainJobAsync(job, dataJob, params, request, triggerType, triggeredBy, startedAt);
        }
        DataJobTriggerResultBO result;
        try {
            result = schedulerDomainService.triggerDataJob(dataJob.jobKey(), DataJobTriggerRequestBO.builder()
                    .triggeredBy(triggeredBy)
                    .dryRun(request != null && Boolean.TRUE.equals(request.getDryRun()))
                    .params(params)
                    .build());
        } catch (RuntimeException exception) {
            OffsetDateTime endedAt = OffsetDateTime.now(CHINA_ZONE);
            recordTriggerResult(job, startedAt, endedAt, false, "failed", exception.getMessage(), triggerType);
            return SchedulerManagementJobActionResponseDTO.builder()
                    .ok(false)
                    .jobId(job.getCode())
                    .accepted(false)
                    .status("failed")
                    .count(0)
                    .failedCount(1)
                    .items(List.of())
                    .error(exception.getMessage())
                    .build();
        }
        List<SchedulerManagementJobActionResponseDTO.TriggerItem> items = toTriggerItems(result.getResult());
        int failedCount = (int) items.stream().filter(item -> !Boolean.TRUE.equals(item.getOk())).count();
        int count = items.isEmpty() ? (Boolean.TRUE.equals(result.getAccepted()) ? 1 : 0) : items.size();
        boolean ok = isTriggerAcceptedSuccess(result);
        String status = normalizeTriggerStatus(result);
        String error = triggerError(result);
        recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), ok, status, error, triggerType, result);
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(ok)
                .jobId(job.getCode())
                .accepted(result.getAccepted())
                .runId(result.getRunId())
                .status(status)
                .count(count)
                .failedCount(ok ? failedCount : Math.max(1, failedCount))
                .items(items)
                .error(error)
                .build();
    }

    private SchedulerManagementJobActionResponseDTO triggerChainJobAsync(SchedulerJobBO job,
                                                                         SchedulerDataJobKeyResolver.ResolvedDataJob dataJob,
                                                                         Map<String, Object> params,
                                                                         SchedulerManagementTriggerRequest request,
                                                                         String triggerType,
                                                                         String triggeredBy,
                                                                         OffsetDateTime startedAt) {
        SchedulerJobStatusBO jobStatus = ensureStatus(job);
        Map<String, Object> extra = new LinkedHashMap<>(readExtra(jobStatus.getExtra()));
        extra.put("lastStep", "starting");
        extra.put("lastCompletedSteps", List.of());
        extra.put("lastStepResults", List.of());
        extra.put("plannedSteps", MARKET_SENTIMENT_CHAIN_STEP_COUNT);
        extra.put("lastMessage", "market_sentiment_chain 已在后台启动");
        extra.put("lastDataJobKey", dataJob.jobKey());
        Map<String, Object> progress = buildProgressEnvelope(extra, "running", startedAt);
        schedulerDomainService.updateStatusProgress(
                jobStatus.getId(),
                true,
                startedAt,
                "running",
                toJson(progress),
                toJson(extra),
                "market_sentiment_chain started");

        Long historyId = schedulerDomainService.createRunHistory(SchedulerJobRunHistoryBO.builder()
                .jobId(job.getId())
                .startedAt(startedAt)
                .endedAt(startedAt)
                .triggerType(triggerType)
                .status("processing")
                .durationSeconds(BigDecimal.ZERO)
                .remark("market_sentiment_chain processing")
                .resultSummary(toJson(buildResultSummary(null, extra, "processing", null, startedAt)))
                .build());

        CompletableFuture.runAsync(() -> {
            try {
                DataJobTriggerResultBO result = schedulerDomainService.triggerDataJob(dataJob.jobKey(), DataJobTriggerRequestBO.builder()
                        .triggerRequestId(job.getCode() + "-" + triggerType + "-" + UUID.randomUUID())
                        .triggeredBy(triggeredBy)
                        .dryRun(request != null && Boolean.TRUE.equals(request.getDryRun()))
                        .params(params)
                        .build());
                if (shouldPollDataJobRun(result)) {
                    pollChainDataJobRun(job, startedAt, triggerType, result, historyId);
                    return;
                }
                boolean ok = isTriggerAcceptedSuccess(result);
                String status = normalizeTriggerStatus(result);
                String error = triggerError(result);
                OffsetDateTime endedAt = OffsetDateTime.now(CHINA_ZONE);
                recordTriggerResult(job, startedAt, endedAt, ok, status, error, triggerType, result, historyId);
            } catch (RuntimeException exception) {
                OffsetDateTime endedAt = OffsetDateTime.now(CHINA_ZONE);
                recordTriggerResult(job, startedAt, endedAt, false, "failed", exception.getMessage(), triggerType, null, historyId);
                log.warn("Async scheduler chain trigger failed: jobCode={}", job.getCode(), exception);
            }
        });

        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(true)
                .jobId(job.getCode())
                .accepted(true)
                .status("processing")
                .count(1)
                .failedCount(0)
                .items(List.of(SchedulerManagementJobActionResponseDTO.TriggerItem.builder()
                        .jobId(job.getCode())
                        .ok(true)
                        .lastStatus("processing")
                        .lastMessage("chain 已在后台启动, 请刷新查看进度")
                        .build()))
                .build();
    }

    private void pollChainDataJobRun(SchedulerJobBO job,
                                     OffsetDateTime startedAt,
                                     String triggerType,
                                     DataJobTriggerResultBO initialResult,
                                     Long historyId) {
        String runId = initialResult.getRunId();
        String jobKey = initialResult.getJobKey();
        long deadlineMs = System.currentTimeMillis() + Duration.ofHours(6).toMillis();
        syncChainProgress(job, startedAt, initialResult);
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
                String status = normalizeTriggerStatus(snapshot);
                if (isTerminalDataJobStatus(status)) {
                    boolean ok = isTriggerAcceptedSuccess(snapshot);
                    String error = triggerError(snapshot);
                    recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), ok, status, error, triggerType, snapshot, historyId);
                    return;
                }
                syncChainProgress(job, startedAt, snapshot);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false, "failed", "poll interrupted", triggerType, initialResult, historyId);
                return;
            } catch (RuntimeException exception) {
                log.warn("Polling chain data job run failed: runId={}", runId, exception);
            }
        }
        recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false, "failed", "poll timeout", triggerType, initialResult, historyId);
    }

    private void syncChainProgress(SchedulerJobBO job,
                                   OffsetDateTime startedAt,
                                   DataJobTriggerResultBO result) {
        SchedulerJobStatusBO jobStatus = ensureStatus(job);
        String status = normalizeTriggerStatus(result);
        Map<String, Object> extra = buildRunExtra(jobStatus, result, false, status, null);
        extra.put("lastRunOk", null);
        extra.put("lastRunError", null);
        extra.putIfAbsent("plannedSteps", MARKET_SENTIMENT_CHAIN_STEP_COUNT);
        Map<String, Object> progress = buildProgressEnvelope(extra, status, OffsetDateTime.now(CHINA_ZONE));
        schedulerDomainService.updateStatusProgress(
                jobStatus.getId(),
                true,
                startedAt,
                status,
                toJson(progress),
                toJson(extra),
                asString(progress.get("message")));
    }

    private DataJobTriggerResultBO dataJobRunSnapshot(String runId, Map<String, Object> run) {
        Map<String, Object> safeRun = run == null ? Map.of() : run;
        return DataJobTriggerResultBO.builder()
                .accepted(true)
                .runId(runId)
                .jobKey(asString(firstPresent(safeRun, Set.of("job_key", "jobKey"))))
                .status(asString(safeRun.get("status")))
                .result(safeRun)
                .build();
    }

    private boolean shouldReconcileDataJobStatus(SchedulerJobStatusBO status) {
        if (status == null || status.getDeletedAt() != null) {
            return false;
        }
        String lastStatus = status.getLastRunStatus();
        return Boolean.TRUE.equals(status.getRunning())
                || "running".equalsIgnoreCase(lastStatus)
                || "processing".equalsIgnoreCase(lastStatus);
    }

    private String extractDataJobRunId(SchedulerJobStatusBO status) {
        Map<String, Object> progress = readExtra(status.getProgress());
        Object fromProgress = readPayloadValue(progress, "lastDataJobRunId");
        if (fromProgress != null) {
            return String.valueOf(fromProgress);
        }
        Map<String, Object> extra = readExtra(status.getExtra());
        Object fromExtra = firstPresent(extra, Set.of("lastDataJobRunId", "runId"));
        return fromExtra == null ? null : String.valueOf(fromExtra);
    }

    private String extractDataJobRunId(String resultSummaryJson) {
        Map<String, Object> summary = readExtra(resultSummaryJson);
        Object fromPayload = readPayloadValue(summary, "lastDataJobRunId");
        return fromPayload == null ? null : String.valueOf(fromPayload);
    }

    private SchedulerJobRunHistoryBO findOpenHistory(Long jobId, String runId) {
        return schedulerDomainService.listRunHistoryByJobId(jobId, 20).stream()
                .filter(history -> history.getDeletedAt() == null)
                .filter(history -> isOpenHistoryStatus(history.getStatus()))
                .filter(history -> runId.equals(extractDataJobRunId(history.getResultSummary())))
                .findFirst()
                .orElse(null);
    }

    private void reconcileOpenRunHistories(SchedulerJobBO job) {
        schedulerDomainService.listRunHistoryByJobId(job.getId(), 20).stream()
                .filter(history -> history.getDeletedAt() == null)
                .filter(history -> isOpenHistoryStatus(history.getStatus()))
                .forEach(history -> reconcileOpenRunHistory(job, history));
    }

    private void reconcileOpenRunHistory(SchedulerJobBO job, SchedulerJobRunHistoryBO history) {
        String runId = extractDataJobRunId(history.getResultSummary());
        if (runId == null || runId.isBlank()) {
            return;
        }
        try {
            Map<String, Object> run = schedulerDomainService.getDataJobRun(runId);
            DataJobTriggerResultBO snapshot = dataJobRunSnapshot(runId, run);
            String status = normalizeTriggerStatus(snapshot);
            if (!isTerminalDataJobStatus(status)) {
                return;
            }
            boolean ok = isTriggerAcceptedSuccess(snapshot);
            String error = triggerError(snapshot);
            OffsetDateTime endedAt = snapshotEndedAt(run);
            recordRunHistoryResult(
                    history,
                    safeStartedAt(snapshotStartedAt(run, null), history.getStartedAt(), endedAt),
                    endedAt,
                    ok,
                    status,
                    error,
                    allowedHistoryTriggerType(history.getTriggerType()),
                    snapshot,
                    history.getId());
        } catch (RuntimeException exception) {
            log.warn("Reconcile scheduler run history failed: jobCode={}, historyId={}, runId={}",
                    job.getCode(), history.getId(), runId, exception);
        }
    }

    private OffsetDateTime snapshotStartedAt(Map<String, Object> run, SchedulerJobStatusBO status) {
        OffsetDateTime fallback = firstNonNull(status == null ? null : status.getLastRunAt(), OffsetDateTime.now(CHINA_ZONE));
        return asOffsetDateTime(run == null ? null : firstPresent(run, Set.of("started_at", "startedAt")), fallback);
    }

    private OffsetDateTime snapshotEndedAt(Map<String, Object> run) {
        return asOffsetDateTime(run == null ? null : firstPresent(run, Set.of("finished_at", "finishedAt")), OffsetDateTime.now(CHINA_ZONE));
    }

    private OffsetDateTime safeStartedAt(OffsetDateTime preferred, OffsetDateTime fallback, OffsetDateTime endedAt) {
        OffsetDateTime startedAt = firstNonNull(firstNonNull(preferred, fallback), firstNonNull(endedAt, OffsetDateTime.now(CHINA_ZONE)));
        if (endedAt != null && startedAt.isAfter(endedAt)) {
            return endedAt;
        }
        return startedAt;
    }

    private String allowedHistoryTriggerType(String triggerType) {
        if ("auto".equals(triggerType)) {
            return "auto";
        }
        return "manual";
    }

    private void recordTriggerResult(SchedulerJobBO job,
                                     OffsetDateTime startedAt,
                                     OffsetDateTime endedAt,
                                     boolean ok,
                                     String status,
                                     String error,
                                     String triggerType) {
        recordTriggerResult(job, startedAt, endedAt, ok, status, error, triggerType, null, null);
    }

    private void recordTriggerResult(SchedulerJobBO job,
                                     OffsetDateTime startedAt,
                                     OffsetDateTime endedAt,
                                     boolean ok,
                                     String status,
                                     String error,
                                     String triggerType,
                                     DataJobTriggerResultBO result) {
        recordTriggerResult(job, startedAt, endedAt, ok, status, error, triggerType, result, null);
    }

    private void recordTriggerResult(SchedulerJobBO job,
                                     OffsetDateTime startedAt,
                                     OffsetDateTime endedAt,
                                     boolean ok,
                                     String status,
                                     String error,
                                     String triggerType,
                                     DataJobTriggerResultBO result,
                                     Long existingHistoryId) {
        BigDecimal durationSeconds = durationSeconds(startedAt, endedAt);
        SchedulerJobStatusBO jobStatus = ensureStatus(job);
        Map<String, Object> extra = buildRunExtra(jobStatus, result, ok, status, error);
        Map<String, Object> progress = buildProgressEnvelope(extra, status, endedAt);
        Map<String, Object> resultSummary = buildResultSummary(result, extra, status, error, endedAt);
        Integer targetsProcessed = extractTargetsProcessed(result, extra);
        String remark = buildRunRemark(result, ok, status, error);
        schedulerDomainService.updateStatusRunResult(
                jobStatus.getId(),
                endedAt,
                ok,
                status,
                error,
                durationSeconds,
                targetsProcessed,
                toJson(progress),
                toJson(extra),
                remark);
        SchedulerJobRunHistoryBO history = SchedulerJobRunHistoryBO.builder()
                .id(existingHistoryId)
                .jobId(job.getId())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .triggerType(triggerType)
                .status(status)
                .errorMessage(error)
                .durationSeconds(durationSeconds)
                .remark(remark)
                .resultSummary(toJson(resultSummary))
                .build();
        if (existingHistoryId == null) {
            schedulerDomainService.createRunHistory(history);
        } else {
            schedulerDomainService.updateRunHistory(history);
        }
    }

    private void recordRunHistoryResult(SchedulerJobRunHistoryBO existingHistory,
                                        OffsetDateTime startedAt,
                                        OffsetDateTime endedAt,
                                        boolean ok,
                                        String status,
                                        String error,
                                        String triggerType,
                                        DataJobTriggerResultBO result,
                                        Long existingHistoryId) {
        BigDecimal durationSeconds = durationSeconds(startedAt, endedAt);
        SchedulerJobStatusBO emptyStatus = SchedulerJobStatusBO.builder().build();
        Map<String, Object> extra = buildRunExtra(emptyStatus, result, ok, status, error);
        Map<String, Object> resultSummary = buildResultSummary(result, extra, status, error, endedAt);
        String remark = buildRunRemark(result, ok, status, error);
        schedulerDomainService.updateRunHistory(SchedulerJobRunHistoryBO.builder()
                .id(existingHistoryId)
                .jobId(existingHistory.getJobId())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .triggerType(triggerType)
                .status(status)
                .errorMessage(error)
                .durationSeconds(durationSeconds)
                .remark(remark)
                .resultSummary(toJson(resultSummary))
                .build());
    }

    private static boolean isTriggerAcceptedSuccess(DataJobTriggerResultBO result) {
        if (result == null || !Boolean.TRUE.equals(result.getAccepted())) {
            return false;
        }
        String status = result.getStatus() == null ? "" : result.getStatus().toLowerCase();
        return "success".equals(status) || "skipped".equals(status);
    }

    private static boolean isAcceptedRunning(DataJobTriggerResultBO result) {
        if (result == null || !Boolean.TRUE.equals(result.getAccepted()) || result.getRunId() == null) {
            return false;
        }
        String status = result.getStatus() == null ? "" : result.getStatus().toLowerCase();
        return "running".equals(status) || "processing".equals(status);
    }

    private static boolean shouldPollDataJobRun(DataJobTriggerResultBO result) {
        if (result == null || result.getRunId() == null || result.getRunId().isBlank()) {
            return false;
        }
        String status = normalizeTriggerStatus(result);
        return !isTerminalDataJobStatus(status) || isAcceptedRunning(result);
    }

    private static boolean isTerminalDataJobStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toLowerCase();
        return "success".equals(normalized)
                || "skipped".equals(normalized)
                || "failed".equals(normalized)
                || "locked".equals(normalized)
                || "unknown_job".equals(normalized);
    }

    private static String normalizeTriggerStatus(DataJobTriggerResultBO result) {
        if (result == null || result.getStatus() == null || result.getStatus().isBlank()) {
            return "failed";
        }
        return result.getStatus();
    }

    private static String triggerError(DataJobTriggerResultBO result) {
        if (result == null || result.getResult() == null) {
            return "missing data job executor result";
        }
        Object errors = result.getResult().get("errors");
        if (errors instanceof List<?> list && !list.isEmpty()) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining("; "));
        }
        if (!isTriggerAcceptedSuccess(result)) {
            Object error = firstPresent(result.getResult(), Set.of("error", "message", "skip_reason"));
            return error == null ? result.getStatus() : String.valueOf(error);
        }
        return null;
    }

    private Map<String, Object> buildRunExtra(SchedulerJobStatusBO jobStatus,
                                              DataJobTriggerResultBO result,
                                              boolean ok,
                                              String status,
                                              String error) {
        Map<String, Object> extra = new LinkedHashMap<>(readExtra(jobStatus.getExtra()));
        Map<String, Object> resultMap = result == null || result.getResult() == null ? Map.of() : result.getResult();
        Map<String, Object> metadata = extractMetadata(resultMap);
        extra.put("lastDataJobRunId", result == null ? null : result.getRunId());
        extra.put("lastDataJobKey", result == null ? null : result.getJobKey());
        extra.put("lastStatus", status);
        extra.put("lastRunOk", ok);
        extra.put("lastRunError", error);
        extra.put("rowsRead", firstNumber(resultMap, metadata, "rows_read", "rowsRead"));
        extra.put("rowsWritten", firstNumber(resultMap, metadata, "rows_written", "rowsWritten"));
        Object pipeline = firstPresent(metadata, Set.of("pipeline"));
        if (pipeline != null) {
            extra.put("pipeline", pipeline);
        }
        copyIfPresent(metadata, extra, "lastStep");
        copyIfPresent(metadata, extra, "lastCompletedSteps");
        copyIfPresent(metadata, extra, "lastStepResults");
        Object plannedSteps = firstNumber(resultMap, metadata, "planned_steps", "plannedSteps");
        if (plannedSteps != null) {
            extra.put("plannedSteps", plannedSteps);
        }
        Object message = firstPresent(metadata, Set.of("lastMessage", "message"));
        if (message == null) {
            Object rowsWritten = extra.get("rowsWritten");
            message = rowsWritten == null ? status : "rows_written=" + rowsWritten;
        }
        extra.put("lastMessage", message);
        return extra;
    }

    private Integer extractTargetsProcessed(DataJobTriggerResultBO result, Map<String, Object> extra) {
        Object completed = extra.get("lastCompletedSteps");
        if (completed instanceof List<?> list && !list.isEmpty()) {
            return list.size();
        }
        Integer rowsWritten = asInteger(extra.get("rowsWritten"));
        if (rowsWritten != null) {
            return rowsWritten;
        }
        if (result != null && result.getResult() != null) {
            Object items = result.getResult().get("items");
            if (items instanceof List<?> list) {
                return list.size();
            }
        }
        return null;
    }

    private String buildRunRemark(DataJobTriggerResultBO result, boolean ok, String status, String error) {
        if (!ok) {
            return error == null || error.isBlank() ? "failed: " + status : error;
        }
        if (result == null) {
            return "success";
        }
        Map<String, Object> resultMap = result.getResult() == null ? Map.of() : result.getResult();
        Map<String, Object> metadata = extractMetadata(resultMap);
        Object rowsRead = firstNumber(resultMap, metadata, "rows_read", "rowsRead");
        Object rowsWritten = firstNumber(resultMap, metadata, "rows_written", "rowsWritten");
        Object message = firstPresent(metadata, Set.of("lastMessage", "message"));
        Object completed = firstPresent(metadata, Set.of("lastCompletedSteps"));
        StringBuilder builder = new StringBuilder();
        builder.append("run_id=").append(result.getRunId());
        builder.append(", data_job=").append(result.getJobKey());
        if (rowsRead != null) {
            builder.append(", rows_read=").append(rowsRead);
        }
        if (rowsWritten != null) {
            builder.append(", rows_written=").append(rowsWritten);
        }
        if (completed instanceof List<?> list && !list.isEmpty()) {
            builder.append(", completed_steps=").append(list.size());
        }
        if (message != null) {
            builder.append(", ").append(message);
        }
        return builder.toString();
    }

    private Map<String, Object> buildProgressEnvelope(Map<String, Object> extra, String status, OffsetDateTime updatedAt) {
        Map<String, Object> safeExtra = extra == null ? Map.of() : extra;
        Map<String, Object> metrics = new LinkedHashMap<>();
        putIfPresent(metrics, "rowsRead", safeExtra.get("rowsRead"));
        putIfPresent(metrics, "rowsWritten", safeExtra.get("rowsWritten"));

        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "lastDataJobRunId", safeExtra.get("lastDataJobRunId"));
        putIfPresent(payload, "lastDataJobKey", safeExtra.get("lastDataJobKey"));
        putIfPresent(payload, "pipeline", safeExtra.get("pipeline"));

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("schemaVersion", 1);
        progress.put("type", inferProgressType(safeExtra));
        progress.put("status", status);
        progress.put("percent", inferPercent(safeExtra, status));
        progress.put("currentStep", firstPresent(safeExtra, Set.of("lastStep", "currentStep")));
        progress.put("completedSteps", firstPresent(safeExtra, Set.of("lastCompletedSteps", "completedSteps")));
        progress.put("stepResults", firstPresent(safeExtra, Set.of("lastStepResults", "stepResults")));
        progress.put("totalSteps", firstPresent(safeExtra, Set.of("plannedSteps", "planned_steps", "totalSteps")));
        progress.put("message", firstPresent(safeExtra, Set.of("lastMessage", "message")));
        progress.put("metrics", metrics);
        progress.put("payload", payload);
        progress.put("updatedAt", updatedAt == null ? OffsetDateTime.now(CHINA_ZONE).toString() : updatedAt.toString());
        return progress;
    }

    private Map<String, Object> buildResultSummary(DataJobTriggerResultBO result,
                                                   Map<String, Object> extra,
                                                   String status,
                                                   String error,
                                                   OffsetDateTime updatedAt) {
        Map<String, Object> safeExtra = extra == null ? Map.of() : extra;
        Map<String, Object> metrics = new LinkedHashMap<>();
        putIfPresent(metrics, "rowsRead", safeExtra.get("rowsRead"));
        putIfPresent(metrics, "rowsWritten", safeExtra.get("rowsWritten"));

        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "lastDataJobRunId", safeExtra.get("lastDataJobRunId"));
        putIfPresent(payload, "lastDataJobKey", safeExtra.get("lastDataJobKey"));
        putIfPresent(payload, "pipeline", safeExtra.get("pipeline"));
        putIfPresent(payload, "completedSteps", firstPresent(safeExtra, Set.of("lastCompletedSteps", "completedSteps")));
        putIfPresent(payload, "stepResults", firstPresent(safeExtra, Set.of("lastStepResults", "stepResults")));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("type", inferProgressType(safeExtra));
        summary.put("status", status);
        Object message = firstPresent(safeExtra, Set.of("lastMessage", "message"));
        summary.put("message", error == null || error.isBlank() ? message : error);
        summary.put("completedCount", completedCount(safeExtra));
        summary.put("failedCount", failedCount(safeExtra, result, status, error));
        summary.put("metrics", metrics);
        summary.put("payload", payload);
        summary.put("updatedAt", updatedAt == null ? OffsetDateTime.now(CHINA_ZONE).toString() : updatedAt.toString());
        return summary;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static String inferProgressType(Map<String, Object> extra) {
        if (extra != null && (extra.containsKey("lastStepResults") || extra.containsKey("lastCompletedSteps"))) {
            return "chain";
        }
        return "job";
    }

    private static BigDecimal inferPercent(Map<String, Object> extra, String status) {
        if ("success".equalsIgnoreCase(status) || "skipped".equalsIgnoreCase(status)) {
            return BigDecimal.valueOf(100);
        }
        if ("failed".equalsIgnoreCase(status)) {
            return null;
        }
        Object completed = extra == null ? null : firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps"));
        Object planned = extra == null ? null : firstPresent(extra, Set.of("plannedSteps", "planned_steps", "totalSteps"));
        Integer plannedCount = asInteger(planned);
        if (completed instanceof List<?> completedList && plannedCount != null && plannedCount > 0) {
            return BigDecimal.valueOf(completedList.size() * 100L)
                    .divide(BigDecimal.valueOf(plannedCount), 2, RoundingMode.HALF_UP);
        }
        Object steps = extra == null ? null : firstPresent(extra, Set.of("lastStepResults", "stepResults"));
        if (completed instanceof List<?> completedList && steps instanceof List<?> stepList && !stepList.isEmpty()) {
            return BigDecimal.valueOf(completedList.size() * 100L)
                    .divide(BigDecimal.valueOf(stepList.size()), 2, RoundingMode.HALF_UP);
        }
        return null;
    }

    private static Integer completedCount(Map<String, Object> extra) {
        Object completed = extra == null ? null : firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps"));
        if (completed instanceof List<?> list) {
            return list.size();
        }
        return null;
    }

    private static Integer failedCount(Map<String, Object> extra, DataJobTriggerResultBO result, String status, String error) {
        Object stepResults = extra == null ? null : firstPresent(extra, Set.of("lastStepResults", "stepResults"));
        if (stepResults instanceof List<?> list) {
            int failed = 0;
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && Boolean.FALSE.equals(asBoolean(map.get("ok")))) {
                    failed++;
                }
            }
            return failed;
        }
        if ("failed".equalsIgnoreCase(status) || (error != null && !error.isBlank())) {
            return 1;
        }
        return result == null ? null : 0;
    }

    private Map<String, Object> extractMetadata(Map<String, Object> resultMap) {
        if (resultMap == null) {
            return Map.of();
        }
        Object metadata = resultMap.get("metadata");
        if (metadata instanceof Map<?, ?> map) {
            return asStringObjectMap(map);
        }
        return Map.of();
    }

    private static Object firstNumber(Map<String, Object> first, Map<String, Object> second, String snakeKey, String camelKey) {
        Object value = first == null ? null : firstPresent(first, Set.of(snakeKey, camelKey));
        if (value != null) {
            return value;
        }
        return second == null ? null : firstPresent(second, Set.of(snakeKey, camelKey));
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source != null && source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private static BigDecimal durationSeconds(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        long millis = Duration.between(startedAt, endedAt).toMillis();
        return BigDecimal.valueOf(millis).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private SchedulerManagementJobActionResponseDTO updateManagementJobEnabled(String jobCode, boolean enabled) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        schedulerDomainService.updateJobEnabled(job.getId(), enabled);
        job.setEnabled(enabled);
        SchedulerJobStatusBO status = ensureStatus(job);
        schedulerDomainService.updateStatusEnabled(status.getId(), enabled);
        status.setEnabled(enabled);
        return actionResponse(job, status, enabled ? "enabled" : "disabled");
    }

    private SchedulerManagementJobActionResponseDTO actionResponse(SchedulerJobBO job,
                                                                   SchedulerJobStatusBO status,
                                                                   String statusText) {
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(true)
                .jobId(job.getCode())
                .enabled(status.getEnabled())
                .status(statusText)
                .count(1)
                .failedCount(0)
                .job(toManagementJobItem(job, status, schedulerDomainService.listCategoryMappings().stream()
                        .filter(mapping -> Objects.equals(mapping.getJobId(), job.getId()))
                        .toList()))
                .build();
    }

    private SchedulerManagementJobActionResponseDTO unsupportedRuntimeAction(SchedulerJobBO job, String action) {
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(false)
                .jobId(job.getCode())
                .status("unsupported")
                .count(0)
                .failedCount(1)
                .error("Java Core does not own a long-running scheduler runtime for " + action
                        + ". Use enable/disable for persisted config or trigger for one-shot execution.")
                .build();
    }

    private SchedulerJobBO requireJobByCode(String jobCode) {
        SchedulerJobBO job = schedulerDomainService.findJobByCode(jobCode);
        if (job == null) {
            throw new IllegalArgumentException("Scheduler job not found: " + jobCode);
        }
        return job;
    }

    private SchedulerJobStatusBO ensureStatus(SchedulerJobBO job) {
        SchedulerJobStatusBO status = schedulerDomainService.findStatusByJobId(job.getId());
        if (status != null) {
            return status;
        }
        SchedulerJobStatusBO created = SchedulerJobStatusBO.builder()
                .jobId(job.getId())
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .running(false)
                .build();
        Long id = schedulerDomainService.createStatus(created);
        created.setId(id);
        return created;
    }

    private SchedulerManagementJobsResponseDTO.Item toManagementJobItem(SchedulerJobBO job,
                                                                        SchedulerJobStatusBO status,
                                                                        List<SchedulerJobCategoryMappingBO> mappings) {
        SchedulerJobStatusBO resolvedStatus = status == null ? SchedulerJobStatusBO.builder()
                .jobId(job.getId())
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .running(false)
                .build() : status;
        List<SchedulerJobCategoryMappingBO> safeMappings = mappings == null ? List.of() : mappings.stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .sorted(Comparator.comparing(SchedulerJobCategoryMappingBO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<String, Integer> categorySortOrders = new HashMap<>();
        for (SchedulerJobCategoryMappingBO mapping : safeMappings) {
            categorySortOrders.put(String.valueOf(mapping.getCategoryId()), mapping.getSortOrder());
        }
        SchedulerManagementJobsResponseDTO.LastRun lastRun = SchedulerManagementJobsResponseDTO.LastRun.builder()
                .lastRunAt(resolvedStatus.getLastRunAt())
                .lastStatus(resolvedStatus.getLastRunStatus())
                .lastTargetsProcessed(resolvedStatus.getLastTargetsProcessed())
                .lastDurationSeconds(resolvedStatus.getLastDurationSeconds())
                .lastError(resolvedStatus.getLastRunErrorMessage())
                .totalRuns(resolvedStatus.getTotalRuns())
                .build();
        SchedulerManagementJobsResponseDTO.Config config = SchedulerManagementJobsResponseDTO.Config.builder()
                .enabled(resolvedStatus.getEnabled())
                .schedule(resolvedStatus.getSchedule())
                .tickSeconds(resolvedStatus.getTickSeconds())
                .timezoneOffsetHours(resolvedStatus.getTimezoneOffsetHours())
                .lastRunAt(resolvedStatus.getLastRunAt())
                .lastStatus(resolvedStatus.getLastRunStatus())
                .lastError(resolvedStatus.getLastRunErrorMessage())
                .lastDurationSeconds(resolvedStatus.getLastDurationSeconds())
                .lastTargetsProcessed(resolvedStatus.getLastTargetsProcessed())
                .totalRuns(resolvedStatus.getTotalRuns())
                .lastStep(firstNonBlank(readString(resolvedStatus.getProgress(), "currentStep"), readString(resolvedStatus.getExtra(), "lastStep")))
                .lastCompletedSteps(firstNonEmpty(readStringList(resolvedStatus.getProgress(), "completedSteps"), readStringList(resolvedStatus.getExtra(), "lastCompletedSteps")))
                .lastStepResults(firstNonEmpty(readChainStepResults(resolvedStatus.getProgress(), "stepResults"), readChainStepResults(resolvedStatus.getExtra(), "lastStepResults")))
                .build();
        SchedulerManagementJobsResponseDTO.Live live = SchedulerManagementJobsResponseDTO.Live.builder()
                .running(Boolean.TRUE.equals(resolvedStatus.getRunning()))
                .schedulerStartedAt(resolvedStatus.getSchedulerStartedAt())
                .stoppedAt(resolvedStatus.getStoppedAt())
                .lastMessage(firstNonBlank(readString(resolvedStatus.getProgress(), "message"), readString(resolvedStatus.getExtra(), "lastMessage")))
                .lastRun(lastRun)
                .build();
        return SchedulerManagementJobsResponseDTO.Item.builder()
                .id(job.getCode())
                .name(job.getName())
                .description(job.getDescription())
                .configFile(job.getConfigFile())
                .serviceModule(job.getServiceModule())
                .serviceClass(job.getServiceClass())
                .registeredAt(job.getRegisteredAt())
                .categories(safeMappings.stream().map(mapping -> String.valueOf(mapping.getCategoryId())).toList())
                .categorySortOrders(categorySortOrders)
                .supportsEnable(true)
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .configEnabled(Boolean.TRUE.equals(resolvedStatus.getEnabled()))
                .config(config)
                .live(live)
                .progress(readProgress(resolvedStatus.getProgress(), resolvedStatus.getExtra(), resolvedStatus.getUpdatedAt()))
                .lastRun(lastRun)
                .build();
    }

    private SchedulerManagementJobHistoryResponseDTO.Item toManagementHistoryItem(SchedulerJobRunHistoryBO item) {
        Integer targetCount = historyTargetCount(item);
        return SchedulerManagementJobHistoryResponseDTO.Item.builder()
                .id(item.getId() == null ? null : String.valueOf(item.getId()))
                .startAt(item.getStartedAt())
                .endAt(item.getEndedAt())
                .triggerType(item.getTriggerType())
                .status(item.getStatus())
                .error(item.getErrorMessage())
                .message(item.getRemark())
                .durationSeconds(item.getDurationSeconds())
                .targetCount(targetCount)
                .succeeded("success".equalsIgnoreCase(item.getStatus()) ? targetCount : null)
                .resultSummary(readResultSummary(item.getResultSummary(), item.getRemark(), item.getUpdatedAt()))
                .build();
    }

    private Integer historyTargetCount(SchedulerJobRunHistoryBO item) {
        if (item == null || item.getRemark() == null) {
            return null;
        }
        Integer completedSteps = parseMetric(item.getRemark(), "completed_steps=");
        if (completedSteps != null && completedSteps > 0) {
            return completedSteps;
        }
        Integer rowsWritten = parseMetric(item.getRemark(), "rows_written=");
        if (rowsWritten != null && rowsWritten > 0) {
            return rowsWritten;
        }
        return null;
    }

    private static Integer parseMetric(String text, String prefix) {
        int start = text == null ? -1 : text.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int index = start + prefix.length();
        StringBuilder digits = new StringBuilder();
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            digits.append(text.charAt(index));
            index++;
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private SchedulerJobStatusBO pickLatestStatus(SchedulerJobStatusBO left, SchedulerJobStatusBO right) {
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

    private SchedulerJobBO pickLatestJob(SchedulerJobBO left, SchedulerJobBO right) {
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

    private String readString(String json, String key) {
        Object value = readExtra(json).get(key);
        return value == null ? null : String.valueOf(value);
    }

    private List<String> readStringList(String json, String key) {
        Object value = readExtra(json).get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private List<SchedulerManagementJobsResponseDTO.ChainStepResult> readChainStepResults(String json, String key) {
        Object value = readExtra(json).get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<SchedulerManagementJobsResponseDTO.ChainStepResult> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(SchedulerManagementJobsResponseDTO.ChainStepResult.builder()
                        .jobId(asString(map.get("jobId")))
                        .ok(asBoolean(map.get("ok")))
                        .lastStatus(asString(map.get("lastStatus")))
                        .lastRunError(asString(map.get("lastRunError")))
                        .lastMessage(asString(map.get("lastMessage")))
                        .build());
            }
        }
        return result;
    }

    private SchedulerManagementJobsResponseDTO.Progress readProgress(String progressJson,
                                                                     String extraJson,
                                                                     OffsetDateTime fallbackUpdatedAt) {
        Map<String, Object> progress = new LinkedHashMap<>(readExtra(progressJson));
        if (progress.isEmpty()) {
            Map<String, Object> extra = readExtra(extraJson);
            if (extra.isEmpty()) {
                return null;
            }
            progress = buildProgressEnvelope(extra, asString(extra.get("lastStatus")), fallbackUpdatedAt);
        }
        return SchedulerManagementJobsResponseDTO.Progress.builder()
                .schemaVersion(asInteger(progress.get("schemaVersion")))
                .type(asString(progress.get("type")))
                .status(asString(progress.get("status")))
                .percent(asBigDecimal(progress.get("percent")))
                .currentStep(asString(progress.get("currentStep")))
                .completedSteps(asStringList(progress.get("completedSteps")))
                .stepResults(readChainStepResults(toJsonMapValue(progress.get("stepResults")), "value"))
                .message(asString(progress.get("message")))
                .metrics(asMap(progress.get("metrics")))
                .payload(asMap(progress.get("payload")))
                .updatedAt(asOffsetDateTime(progress.get("updatedAt"), fallbackUpdatedAt))
                .build();
    }

    private SchedulerManagementJobHistoryResponseDTO.ResultSummary readResultSummary(String summaryJson,
                                                                                     String fallbackMessage,
                                                                                     OffsetDateTime fallbackUpdatedAt) {
        Map<String, Object> summary = readExtra(summaryJson);
        if (summary.isEmpty()) {
            return fallbackMessage == null ? null : SchedulerManagementJobHistoryResponseDTO.ResultSummary.builder()
                    .schemaVersion(1)
                    .type("job")
                    .message(fallbackMessage)
                    .updatedAt(fallbackUpdatedAt)
                    .build();
        }
        return SchedulerManagementJobHistoryResponseDTO.ResultSummary.builder()
                .schemaVersion(asInteger(summary.get("schemaVersion")))
                .type(asString(summary.get("type")))
                .status(asString(summary.get("status")))
                .message(asString(summary.get("message")))
                .completedCount(asInteger(summary.get("completedCount")))
                .failedCount(asInteger(summary.get("failedCount")))
                .metrics(asMap(summary.get("metrics")))
                .payload(asMap(summary.get("payload")))
                .updatedAt(asOffsetDateTime(summary.get("updatedAt"), fallbackUpdatedAt))
                .build();
    }

    private String toJsonMapValue(Object value) {
        return toJson(Map.of("value", value == null ? List.of() : value));
    }

    private List<SchedulerManagementJobActionResponseDTO.TriggerItem> toTriggerItems(Map<String, Object> result) {
        Object rawItems = result == null ? null : result.get("items");
        if (!(rawItems instanceof List<?>)) {
            Map<String, Object> metadata = extractMetadata(result);
            rawItems = metadata.get("lastStepResults");
        }
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }
        List<SchedulerManagementJobActionResponseDTO.TriggerItem> items = new ArrayList<>();
        for (Object raw : list) {
            if (raw instanceof Map<?, ?> map) {
                items.add(SchedulerManagementJobActionResponseDTO.TriggerItem.builder()
                        .jobId(asString(map.get("jobId")))
                        .ok(asBoolean(map.get("ok")))
                        .lastStatus(asString(map.get("lastStatus")))
                        .lastRunError(asString(firstPresent(map, Set.of("lastRunError", "last_error"))))
                        .lastMessage(asString(map.get("lastMessage")))
                        .build());
            }
        }
        return items;
    }

    private Map<String, Object> readExtra(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return asStringObjectMap(objectMapper.readValue(json, Map.class));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static Map<String, Object> asStringObjectMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static Object firstPresent(Map<?, ?> map, Set<String> keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static Object readPayloadValue(Map<String, Object> envelope, String key) {
        if (envelope == null) {
            return null;
        }
        Object payload = envelope.get("payload");
        if (payload instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first == null ? second : first;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static <T> List<T> firstNonEmpty(List<T> first, List<T> second) {
        return first != null && !first.isEmpty() ? first : second;
    }

    private static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return asStringObjectMap(map);
        }
        return Map.of();
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null) {
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static OffsetDateTime asOffsetDateTime(Object value, OffsetDateTime fallback) {
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value != null) {
            try {
                return OffsetDateTime.parse(String.valueOf(value));
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean isOpenHistoryStatus(String status) {
        return "running".equalsIgnoreCase(status)
                || "processing".equalsIgnoreCase(status);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static class DayCounter {
        private int total;
        private int success;
        private int failed;
        private int skipped;

        private void accept(String status) {
            total++;
            String normalized = status == null ? "" : status.toLowerCase();
            if ("success".equals(normalized)) {
                success++;
            } else if ("skipped".equals(normalized)) {
                skipped++;
            } else if ("failed".equals(normalized) || "error".equals(normalized) || "partial_failed".equals(normalized)) {
                failed++;
            }
        }
    }
}
