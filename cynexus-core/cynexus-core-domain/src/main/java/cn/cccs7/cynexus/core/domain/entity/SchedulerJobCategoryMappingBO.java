package cn.cccs7.cynexus.core.domain.entity;

public record SchedulerJobCategoryMappingBO(
        Long id,
        Long jobId,
        Long categoryId,
        String remark,
        Integer sortOrder
) {
}
