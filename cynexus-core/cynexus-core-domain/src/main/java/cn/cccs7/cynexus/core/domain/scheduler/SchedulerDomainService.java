package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface SchedulerDomainService {

    List<SchedulerJobBO> listJobs();

    SchedulerJobBO findJobById(Long id);

    SchedulerJobBO findJobByCode(String code);

    Long createJob(SchedulerJobBO job);

    boolean updateJob(SchedulerJobBO job);

    boolean updateJobEnabled(Long id, Boolean enabled);

    boolean deleteJob(Long id);

    List<SchedulerJobCategoryBO> listCategories();

    SchedulerJobCategoryBO findCategoryById(Long id);

    Long createCategory(SchedulerJobCategoryBO category);

    boolean updateCategory(SchedulerJobCategoryBO category);

    boolean deleteCategory(Long id);

    List<SchedulerJobCategoryMappingBO> listCategoryMappings();

    List<SchedulerJobCategoryMappingBO> listCategoryMappingsByJobId(Long jobId);

    SchedulerJobCategoryMappingBO findCategoryMappingById(Long id);

    Long createCategoryMapping(SchedulerJobCategoryMappingBO mapping);

    boolean updateCategoryMapping(SchedulerJobCategoryMappingBO mapping);

    boolean deleteCategoryMapping(Long id);

    List<SchedulerJobRunHistoryBO> listRunHistory();

    List<SchedulerJobRunHistoryBO> listRunHistoryByJobId(Long jobId, Integer limit);

    List<SchedulerJobRunHistoryBO> listRunHistorySince(OffsetDateTime startedAt);

    SchedulerJobRunHistoryBO findRunHistoryById(Long id);

    Long createRunHistory(SchedulerJobRunHistoryBO runHistory);

    boolean updateRunHistory(SchedulerJobRunHistoryBO runHistory);

    boolean deleteRunHistory(Long id);

    List<SchedulerJobStatusBO> listStatuses();

    SchedulerJobStatusBO findStatusByJobId(Long jobId);

    SchedulerJobStatusBO findStatusById(Long id);

    Long createStatus(SchedulerJobStatusBO status);

    boolean updateStatus(SchedulerJobStatusBO status);

    boolean updateStatusEnabled(Long id, Boolean enabled);

    boolean updateStatusRunning(Long id,
                                Boolean running,
                                OffsetDateTime schedulerStartedAt,
                                OffsetDateTime stoppedAt);

    boolean updateStatusRunResult(Long id,
                                  OffsetDateTime lastRunAt,
                                  Boolean lastRunOk,
                                  String lastRunStatus,
                                  String lastRunErrorMessage,
                                  BigDecimal lastDurationSeconds);

    boolean updateStatusRunResult(Long id,
                                  OffsetDateTime lastRunAt,
                                  Boolean lastRunOk,
                                  String lastRunStatus,
                                  String lastRunErrorMessage,
                                  BigDecimal lastDurationSeconds,
                                  Integer lastTargetsProcessed,
                                  String progress,
                                  String extra,
                                  String remark);

    boolean updateStatusProgress(Long id,
                                 Boolean running,
                                 OffsetDateTime lastRunAt,
                                 String lastRunStatus,
                                 String progress,
                                 String extra,
                                 String remark);

    boolean deleteStatus(Long id);

    Map<String, Object> dataJobExecutorHealth();

    Map<String, Object> listDataJobs();

    Map<String, Object> getDataJob(String jobKey);

    DataJobTriggerResultBO triggerDataJob(String jobKey, DataJobTriggerRequestBO request);

    Map<String, Object> getDataJobRun(String runId);

    Map<String, Object> startSocialLogin(String platform, String profileName);

    Map<String, Object> getSocialLogin(String sessionId);

    Map<String, Object> completeSocialLogin(String sessionId);

    Map<String, Object> closeSocialLogin(String sessionId);
}
