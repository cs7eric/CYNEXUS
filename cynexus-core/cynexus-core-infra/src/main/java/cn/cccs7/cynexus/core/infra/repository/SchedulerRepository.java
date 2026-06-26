package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.util.List;

public interface SchedulerRepository {

    List<SchedulerJobBO> listJobs();

    List<SchedulerJobCategoryBO> listCategories();

    List<SchedulerJobCategoryMappingBO> listCategoryMappings();

    List<SchedulerJobRunHistoryBO> listRunHistory();

    List<SchedulerJobStatusBO> listStatuses();
}
