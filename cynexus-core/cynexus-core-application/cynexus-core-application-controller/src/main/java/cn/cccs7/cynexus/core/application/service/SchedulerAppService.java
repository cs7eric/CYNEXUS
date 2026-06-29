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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchedulerAppService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

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
        SchedulerJobBO job = requireJobByCode(jobCode);
        OffsetDateTime startedAt = OffsetDateTime.now(CHINA_ZONE);
        String dataJobKey = SchedulerDataJobKeyResolver.resolve(job.getCode()).orElse(null);
        if (dataJobKey == null) {
            String error = "No Python data job mapping for scheduler job: " + job.getCode();
            recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), false, "failed", error);
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
        Map<String, Object> params = new LinkedHashMap<>();
        if (request != null && request.getTargetDate() != null && !request.getTargetDate().isBlank()) {
            params.put("target_date", request.getTargetDate());
        }
        DataJobTriggerResultBO result;
        try {
            result = schedulerDomainService.triggerDataJob(dataJobKey, DataJobTriggerRequestBO.builder()
                    .triggeredBy("java-scheduler-management")
                    .dryRun(request != null && Boolean.TRUE.equals(request.getDryRun()))
                    .params(params)
                    .build());
        } catch (RuntimeException exception) {
            OffsetDateTime endedAt = OffsetDateTime.now(CHINA_ZONE);
            recordTriggerResult(job, startedAt, endedAt, false, "failed", exception.getMessage());
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
        recordTriggerResult(job, startedAt, OffsetDateTime.now(CHINA_ZONE), ok, status, error);
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

    private void recordTriggerResult(SchedulerJobBO job,
                                     OffsetDateTime startedAt,
                                     OffsetDateTime endedAt,
                                     boolean ok,
                                     String status,
                                     String error) {
        BigDecimal durationSeconds = durationSeconds(startedAt, endedAt);
        SchedulerJobStatusBO jobStatus = ensureStatus(job);
        schedulerDomainService.updateStatusRunResult(
                jobStatus.getId(),
                endedAt,
                ok,
                status,
                error,
                durationSeconds);
        schedulerDomainService.createRunHistory(SchedulerJobRunHistoryBO.builder()
                .jobId(job.getId())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .triggerType("manual")
                .status(status)
                .errorMessage(error)
                .durationSeconds(durationSeconds)
                .remark(ok ? "Triggered by Java scheduler management" : "Trigger failed in Java scheduler management")
                .build());
    }

    private static boolean isTriggerAcceptedSuccess(DataJobTriggerResultBO result) {
        if (result == null || !Boolean.TRUE.equals(result.getAccepted())) {
            return false;
        }
        String status = result.getStatus() == null ? "" : result.getStatus().toLowerCase();
        return "success".equals(status) || "skipped".equals(status);
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
                .lastStep(readString(resolvedStatus.getExtra(), "lastStep"))
                .lastCompletedSteps(readStringList(resolvedStatus.getExtra(), "lastCompletedSteps"))
                .lastStepResults(readChainStepResults(resolvedStatus.getExtra(), "lastStepResults"))
                .build();
        SchedulerManagementJobsResponseDTO.Live live = SchedulerManagementJobsResponseDTO.Live.builder()
                .running(Boolean.TRUE.equals(resolvedStatus.getRunning()))
                .schedulerStartedAt(resolvedStatus.getSchedulerStartedAt())
                .stoppedAt(resolvedStatus.getStoppedAt())
                .lastMessage(readString(resolvedStatus.getExtra(), "lastMessage"))
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
                .lastRun(lastRun)
                .build();
    }

    private SchedulerManagementJobHistoryResponseDTO.Item toManagementHistoryItem(SchedulerJobRunHistoryBO item) {
        return SchedulerManagementJobHistoryResponseDTO.Item.builder()
                .id(item.getId() == null ? null : String.valueOf(item.getId()))
                .startAt(item.getStartedAt())
                .endAt(item.getEndedAt())
                .triggerType(item.getTriggerType())
                .status(item.getStatus())
                .error(item.getErrorMessage())
                .message(item.getRemark())
                .durationSeconds(item.getDurationSeconds())
                .build();
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

    private List<SchedulerManagementJobActionResponseDTO.TriggerItem> toTriggerItems(Map<String, Object> result) {
        Object rawItems = result == null ? null : result.get("items");
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

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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
