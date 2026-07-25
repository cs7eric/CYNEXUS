package cn.cccs7.cynexus.core.domain.valueobject.scheduler;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;

public record SchedulerCategoryMutationResult(SchedulerJobCategoryBO category, int mappingCount) {
}
