package cn.cccs7.cynexus.core.domain.service.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryCreateCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryDeleteOutcome;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryMutationResult;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryUpdateCommand;
import java.util.List;
import java.util.Set;

/** Coordinates scheduler configuration and lifecycle changes across aggregates. */
public interface SchedulerManagementDomainService {

    SchedulerJobBO requireJobByCode(String jobCode);

    SchedulerJobStatusBO ensureStatus(SchedulerJobBO job);

    SchedulerJobStatusBO setEnabled(SchedulerJobBO job, boolean enabled);

    SchedulerCategoryMutationResult createCategory(SchedulerCategoryCreateCommand command);

    SchedulerCategoryMutationResult updateCategory(Long categoryId, SchedulerCategoryUpdateCommand command);

    SchedulerCategoryDeleteOutcome deleteCategory(Long categoryId);

    boolean deleteJob(SchedulerJobBO job);

    List<SchedulerJobCategoryMappingBO> replaceCategoryMappings(
            SchedulerJobBO job, Set<Long> requestedCategoryIds);
}
