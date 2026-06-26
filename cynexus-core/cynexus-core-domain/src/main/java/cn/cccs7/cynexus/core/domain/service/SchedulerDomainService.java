package cn.cccs7.cynexus.core.domain.service;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.util.List;

public interface SchedulerDomainService {

    List<SchedulerJobBO> listJobs();

    SchedulerJobBO findJobById(Long id);

    Long createJob(SchedulerJobBO job);

    boolean updateJob(SchedulerJobBO job);

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

    SchedulerJobRunHistoryBO findRunHistoryById(Long id);

    Long createRunHistory(SchedulerJobRunHistoryBO runHistory);

    boolean updateRunHistory(SchedulerJobRunHistoryBO runHistory);

    boolean deleteRunHistory(Long id);

    List<SchedulerJobStatusBO> listStatuses();

    SchedulerJobStatusBO findStatusById(Long id);

    Long createStatus(SchedulerJobStatusBO status);

    boolean updateStatus(SchedulerJobStatusBO status);

    boolean deleteStatus(Long id);
}
