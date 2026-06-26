package cn.cccs7.cynexus.core.domain.entity;

public record SchedulerJobCategoryBO(
        Long id,
        String code,
        String label,
        String iconHint,
        Integer sortOrder,
        String description,
        String status,
        String remark
) {
}
