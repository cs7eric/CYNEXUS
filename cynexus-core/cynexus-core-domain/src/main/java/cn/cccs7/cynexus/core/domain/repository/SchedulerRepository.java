package cn.cccs7.cynexus.core.domain.repository;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface SchedulerRepository {

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

    boolean updateStatusRunResult(Long id,
                                  OffsetDateTime lastRunAt,
                                  Boolean lastRunOk,
                                  String lastRunStatus,
                                  String lastRunErrorMessage,
                                  BigDecimal lastDurationSeconds);

    boolean deleteStatus(Long id);
}
