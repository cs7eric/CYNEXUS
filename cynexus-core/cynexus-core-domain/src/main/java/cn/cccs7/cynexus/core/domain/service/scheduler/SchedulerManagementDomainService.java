package cn.cccs7.cynexus.core.domain.service.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.util.List;
import java.util.Set;

/** Coordinates scheduler configuration and lifecycle changes across aggregates. */
public interface SchedulerManagementDomainService {

    SchedulerJobBO requireJobByCode(String jobCode);

    SchedulerJobStatusBO ensureStatus(SchedulerJobBO job);

    SchedulerJobStatusBO setEnabled(SchedulerJobBO job, boolean enabled);

    boolean deleteJob(SchedulerJobBO job);

    List<SchedulerJobCategoryMappingBO> replaceCategoryMappings(
            SchedulerJobBO job, Set<Long> requestedCategoryIds);
}
