package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.common.constant.CoreConstants;
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
import cn.cccs7.cynexus.core.application.request.SchedulerManagementCategoryCreateRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementCategoryUpdateRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementJobCategoriesRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementTriggerRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerJobCreateRequest;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerDailyCounterBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerTodayCountersBO;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerManagementPolicy;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerRunResultDomainPolicy;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerReconciliationPolicy;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerExecutionDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerTriggerDomainService;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryCreateCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryDeleteOutcome;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryMutationResult;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryUpdateCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerOutcome;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.scheduling.support.CronExpression;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchedulerAppService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerAppService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);
    private final SchedulerDomainService schedulerDomainService;
    private final SchedulerManagementDomainService schedulerManagementDomainService;
    private final SchedulerExecutionDomainService schedulerExecutionDomainService;
    private final SchedulerTriggerDomainService schedulerTriggerDomainService;
    private final ObjectMapper objectMapper;
    private final SchedulerRunResultDomainPolicy runResultPolicy = new SchedulerRunResultDomainPolicy();

    public SchedulerAppService(SchedulerDomainService schedulerDomainService,
                               SchedulerManagementDomainService schedulerManagementDomainService,
                               SchedulerExecutionDomainService schedulerExecutionDomainService,
                               SchedulerTriggerDomainService schedulerTriggerDomainService) {
        this.schedulerDomainService = schedulerDomainService;
        this.schedulerManagementDomainService = schedulerManagementDomainService;
        this.schedulerExecutionDomainService = schedulerExecutionDomainService;
        this.schedulerTriggerDomainService = schedulerTriggerDomainService;
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

        LocalDate today = LocalDate.now(CHINA_ZONE);
        OffsetDateTime startOfToday = today.atStartOfDay(CHINA_ZONE).toOffsetDateTime();
        Map<Long, SchedulerTodayCountersBO> todayCountersByJobId = SchedulerManagementPolicy.todayCounters(
                schedulerDomainService.listRunHistorySince(startOfToday));

        List<SchedulerManagementJobsResponseDTO.Item> items = jobs.stream()
                .map(job -> toManagementJobItem(
                        job,
                        statusByJobId.get(job.getId()),
                        mappingsByJobId.get(job.getId()),
                        todayCountersByJobId.getOrDefault(job.getId(), new SchedulerTodayCountersBO()),
                        today))
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
                .map(category -> toManagementCategoryItem(
                        category,
                        countByCategoryId.getOrDefault(category.getId(), 0L).intValue()))
                .toList();
        return SchedulerManagementCategoriesResponseDTO.builder()
                .ok(true)
                .items(items)
                .count(items.size())
                .build();
    }

    public SchedulerManagementCategoriesResponseDTO.Item createManagementCategory(
            SchedulerManagementCategoryCreateRequest request) {
        SchedulerCategoryMutationResult result = schedulerManagementDomainService.createCategory(
                new SchedulerCategoryCreateCommand(
                        request == null ? null : request.getCode(),
                        request == null ? null : request.getLabel(),
                        request == null ? null : request.getIconHint(),
                        request == null ? null : request.getSortOrder(),
                        request == null ? null : request.getDescription()));
        return toManagementCategoryItem(result.category(), result.mappingCount());
    }

    public SchedulerManagementCategoriesResponseDTO.Item updateManagementCategory(
            Long categoryId,
            SchedulerManagementCategoryUpdateRequest request) {
        SchedulerCategoryMutationResult result = schedulerManagementDomainService.updateCategory(categoryId,
                new SchedulerCategoryUpdateCommand(
                        request == null ? null : request.getCode(),
                        request == null ? null : request.getLabel(),
                        request == null ? null : request.getIconHint(),
                        request == null ? null : request.getSortOrder(),
                        request == null ? null : request.getDescription()));
        return toManagementCategoryItem(result.category(), result.mappingCount());
    }

    public SchedulerManagementJobActionResponseDTO deleteManagementCategory(Long categoryId) {
        SchedulerCategoryDeleteOutcome outcome = schedulerManagementDomainService.deleteCategory(categoryId);
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(outcome.deleted())
                .status(outcome.deleted() ? "category_deleted" : "category_delete_failed")
                .count(outcome.deleted() ? outcome.deletedMappingCount() + 1 : 0)
                .failedCount(outcome.deleted() ? 0 : 1)
                .build();
    }

    private SchedulerManagementCategoriesResponseDTO.Item toManagementCategoryItem(SchedulerJobCategoryBO category,
                                                                                   int count) {
        return SchedulerManagementCategoriesResponseDTO.Item.builder()
                .id(String.valueOf(category.getId()))
                .label(category.getLabel())
                .iconHint(category.getIconHint())
                .sortOrder(category.getSortOrder())
                .description(category.getDescription())
                .count(count)
                .build();
    }

    public SchedulerManagementDailyStatsResponseDTO listManagementDailyStats(int days) {
        int boundedDays = SchedulerManagementPolicy.boundedDays(days, 90);
        LocalDate end = LocalDate.now(CHINA_ZONE);
        LocalDate start = end.minusDays(boundedDays - 1L);
        OffsetDateTime startedAt = start.atStartOfDay(CHINA_ZONE).toOffsetDateTime();
        List<SchedulerJobRunHistoryBO> history = schedulerDomainService.listRunHistorySince(startedAt);
        Map<LocalDate, SchedulerDailyCounterBO> counters = SchedulerManagementPolicy.dailyCounters(history, start, boundedDays);
        List<SchedulerManagementDailyStatsResponseDTO.Item> items = counters.entrySet().stream()
                .map(entry -> SchedulerManagementDailyStatsResponseDTO.Item.builder()
                        .date(entry.getKey())
                        .total(entry.getValue().getTotal())
                        .success(entry.getValue().getSuccess())
                        .failed(entry.getValue().getFailed())
                        .skipped(entry.getValue().getSkipped())
                        .manualCount(entry.getValue().getManual())
                        .autoCount(entry.getValue().getAuto())
                        .build())
                .toList();
        int total = counters.values().stream().mapToInt(SchedulerDailyCounterBO::getTotal).sum();
        int failed = counters.values().stream().mapToInt(SchedulerDailyCounterBO::getFailed).sum();
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
        int boundedLimit = SchedulerManagementPolicy.boundedLimit(limit, 200);
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

    public SchedulerManagementJobActionResponseDTO updateManagementJobCategories(
            String jobCode,
            SchedulerManagementJobCategoriesRequest request) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        LinkedHashSet<Long> requestedCategoryIds = parseCategoryIds(request);
        List<SchedulerJobCategoryMappingBO> mappings = schedulerManagementDomainService
                .replaceCategoryMappings(job, requestedCategoryIds);

        SchedulerJobStatusBO status = schedulerManagementDomainService.ensureStatus(job);
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(true)
                .jobId(job.getCode())
                .enabled(status.getEnabled())
                .status("categories_updated")
                .count(mappings.size())
                .failedCount(0)
                .job(toManagementJobItem(job, status, mappings, new SchedulerTodayCountersBO(), LocalDate.now(CHINA_ZONE)))
                .build();
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
        boolean deleted = schedulerManagementDomainService.deleteJob(job);
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
        schedulerExecutionDomainService.recoverExpiredRunningStates(
                OffsetDateTime.now(CHINA_ZONE), Duration.ofHours(8));
        Map<Long, SchedulerJobBO> jobsById = schedulerDomainService.listJobs().stream()
                .filter(job -> job.getDeletedAt() == null)
                .collect(Collectors.toMap(SchedulerJobBO::getId, Function.identity(), this::pickLatestJob));
        List<SchedulerJobStatusBO> statuses = schedulerDomainService.listStatuses().stream()
                .filter(status -> status.getDeletedAt() == null)
                .toList();
        statuses.stream()
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
            String finalStatus = SchedulerManagementPolicy.normalizeTriggerStatus(snapshot);
            if (!SchedulerManagementPolicy.isTerminalStatus(finalStatus)) {
                schedulerTriggerDomainService.synchronizeProgress(
                        job, firstNonNull(status.getSchedulerStartedAt(), snapshotStartedAt(run, status)), snapshot);
                return;
            }
            boolean ok = SchedulerManagementPolicy.isAcceptedSuccess(snapshot);
            String error = SchedulerManagementPolicy.triggerError(snapshot);
            OffsetDateTime endedAt = snapshotEndedAt(run);
            OffsetDateTime startedAt = safeStartedAt(snapshotStartedAt(run, status), status.getSchedulerStartedAt(), endedAt);
            String triggerType = openHistory == null ? "manual" : SchedulerManagementPolicy.allowedHistoryTriggerType(openHistory.getTriggerType());
            schedulerTriggerDomainService.recordResult(
                    job, startedAt, endedAt, ok, finalStatus, error, triggerType, snapshot, historyId);
        } catch (RuntimeException exception) {
            log.warn("Reconcile scheduler data job run failed: jobCode={}, runId={}", job.getCode(), runId, exception);
        }
    }

    private SchedulerManagementJobActionResponseDTO triggerJob(String jobCode,
                                                               SchedulerManagementTriggerRequest request,
                                                               String triggerType,
                                                               String triggeredBy) {
        SchedulerTriggerOutcome outcome = schedulerTriggerDomainService.trigger(new SchedulerTriggerCommand(
                jobCode,
                request == null ? null : request.getTargetDate(),
                request != null && Boolean.TRUE.equals(request.getDryRun()),
                triggerType,
                triggeredBy,
                OffsetDateTime.now(CHINA_ZONE)));
        return SchedulerManagementJobActionResponseDTO.builder()
                .ok(outcome.ok())
                .jobId(outcome.jobCode())
                .accepted(outcome.accepted())
                .runId(outcome.runId())
                .status(outcome.status())
                .count(outcome.count())
                .failedCount(outcome.failedCount())
                .items(outcome.items().stream()
                        .map(item -> SchedulerManagementJobActionResponseDTO.TriggerItem.builder()
                                .jobId(item.jobId())
                                .ok(item.ok())
                                .lastStatus(item.lastStatus())
                                .lastRunError(item.lastRunError())
                                .lastMessage(item.lastMessage())
                                .build())
                        .toList())
                .error(outcome.error())
                .build();
    }

    private DataJobTriggerResultBO dataJobRunSnapshot(String runId, Map<String, Object> run) {
        return SchedulerReconciliationPolicy.dataJobRunSnapshot(runId, run);
    }

    private boolean shouldReconcileDataJobStatus(SchedulerJobStatusBO status) {
        return SchedulerReconciliationPolicy.shouldReconcileDataJobStatus(status);
    }

    private String extractDataJobRunId(SchedulerJobStatusBO status) {
        if (status == null) {
            return null;
        }
        return SchedulerReconciliationPolicy.extractRunId(readExtra(status.getProgress()), readExtra(status.getExtra()));
    }

    private String extractDataJobRunId(String resultSummaryJson) {
        return SchedulerReconciliationPolicy.extractRunIdFromSummary(readExtra(resultSummaryJson));
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
            String status = SchedulerManagementPolicy.normalizeTriggerStatus(snapshot);
            if (!SchedulerManagementPolicy.isTerminalStatus(status)) {
                return;
            }
            boolean ok = SchedulerManagementPolicy.isAcceptedSuccess(snapshot);
            String error = SchedulerManagementPolicy.triggerError(snapshot);
            OffsetDateTime endedAt = snapshotEndedAt(run);
            recordRunHistoryResult(
                    history,
                    safeStartedAt(snapshotStartedAt(run, null), history.getStartedAt(), endedAt),
                    endedAt,
                    ok,
                    status,
                    error,
                    SchedulerManagementPolicy.allowedHistoryTriggerType(history.getTriggerType()),
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
        return SchedulerReconciliationPolicy.safeStartedAt(preferred, fallback, endedAt, OffsetDateTime.now(CHINA_ZONE));
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
        BigDecimal durationSeconds = runResultPolicy.durationSeconds(startedAt, endedAt);
        SchedulerJobStatusBO emptyStatus = SchedulerJobStatusBO.builder().build();
        SchedulerRunResultDomainPolicy.RunFacts facts = runResultPolicy.build(emptyStatus, result, ok, status, error, endedAt);
        Map<String, Object> resultSummary = facts.resultSummary();
        String remark = facts.remark();
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

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private SchedulerManagementJobActionResponseDTO updateManagementJobEnabled(String jobCode, boolean enabled) {
        SchedulerJobBO job = requireJobByCode(jobCode);
        SchedulerJobStatusBO status = schedulerManagementDomainService.setEnabled(job, enabled);
        return actionResponse(job, status, enabled ? "enabled" : "disabled");
    }

    private LinkedHashSet<Long> parseCategoryIds(SchedulerManagementJobCategoriesRequest request) {
        LinkedHashSet<Long> categoryIds = new LinkedHashSet<>();
        if (request == null || request.getCategoryIds() == null) {
            return categoryIds;
        }
        for (String raw : request.getCategoryIds()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                categoryIds.add(Long.parseLong(raw.trim()));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid scheduler category id: " + raw);
            }
        }
        return categoryIds;
    }

    private SchedulerJobCategoryBO pickCategory(SchedulerJobCategoryBO first, SchedulerJobCategoryBO second) {
        return first.getUpdatedAt() == null ? second : first;
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
                        .toList(),
                        new SchedulerTodayCountersBO(),
                        LocalDate.now(CHINA_ZONE)))
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
        return schedulerManagementDomainService.requireJobByCode(jobCode);
    }

    private SchedulerJobStatusBO ensureStatus(SchedulerJobBO job) {
        return schedulerManagementDomainService.ensureStatus(job);
    }

    private SchedulerManagementJobsResponseDTO.Item toManagementJobItem(SchedulerJobBO job,
                                                                        SchedulerJobStatusBO status,
                                                                        List<SchedulerJobCategoryMappingBO> mappings,
                                                                        SchedulerTodayCountersBO todayCounters,
                                                                        LocalDate today) {
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
        Integer expectedCount = SchedulerManagementPolicy.expectedCount(readExtra(resolvedStatus.getSchedule()), today);
        SchedulerManagementJobsResponseDTO.TodayStats todayStats = SchedulerManagementJobsResponseDTO.TodayStats.builder()
                .expectedCount(expectedCount)
                .actualCount(todayCounters.getActual())
                .successCount(todayCounters.getSuccess())
                .failedCount(todayCounters.getFailed())
                .skippedCount(todayCounters.getSkipped())
                .manualCount(todayCounters.getManual())
                .autoCount(todayCounters.getAuto())
                .failedAt(todayCounters.getFailedAt())
                .failedError(todayCounters.getFailedError())
                .failedMessage(todayCounters.getFailedMessage())
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
                .configEnabled(resolvedStatus.getEnabled() != null
                        ? Boolean.TRUE.equals(resolvedStatus.getEnabled())
                        : Boolean.TRUE.equals(job.getEnabled()))
                .config(config)
                .live(live)
                .progress(readProgress(resolvedStatus.getProgress(), resolvedStatus.getExtra(), resolvedStatus.getUpdatedAt()))
                .lastRun(lastRun)
                .todayStats(todayStats)
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
                .durationSeconds(historyDurationSeconds(item))
                .targetCount(targetCount)
                .succeeded(CoreConstants.STATUS_SUCCESS.equalsIgnoreCase(item.getStatus()) ? targetCount : null)
                .resultSummary(readResultSummary(item.getResultSummary(), item.getRemark(), item.getUpdatedAt()))
                .build();
    }

    private BigDecimal historyDurationSeconds(SchedulerJobRunHistoryBO item) {
        if (item == null) {
            return null;
        }
        if (isOpenHistoryStatus(item.getStatus()) && item.getStartedAt() != null) {
            return runResultPolicy.durationSeconds(item.getStartedAt(), OffsetDateTime.now(CHINA_ZONE));
        }
        return item.getDurationSeconds();
    }

    private Integer historyTargetCount(SchedulerJobRunHistoryBO item) {
        if (item == null || item.getRemark() == null) {
            return null;
        }
        Integer completedSteps = SchedulerManagementPolicy.metric(item.getRemark(), "completed_steps=");
        if (completedSteps != null && completedSteps > 0) {
            return completedSteps;
        }
        Integer rowsWritten = SchedulerManagementPolicy.metric(item.getRemark(), "rows_written=");
        if (rowsWritten != null && rowsWritten > 0) {
            return rowsWritten;
        }
        return null;
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
            progress = runResultPolicy.progress(extra, asString(extra.get("lastStatus")), fallbackUpdatedAt);
        }
        return SchedulerManagementJobsResponseDTO.Progress.builder()
                .schemaVersion(asInteger(progress.get("schemaVersion")))
                .type(asString(progress.get("type")))
                .status(asString(progress.get("status")))
                .percent(asBigDecimal(progress.get("percent")))
                .currentStep(asString(progress.get("currentStep")))
                .currentStepProgress(asMap(progress.get("currentStepProgress")))
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
        return SchedulerReconciliationPolicy.isOpenHistoryStatus(status);
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

}
