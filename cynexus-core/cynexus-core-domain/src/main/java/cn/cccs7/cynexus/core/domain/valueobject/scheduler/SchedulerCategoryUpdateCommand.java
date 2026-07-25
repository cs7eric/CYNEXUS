package cn.cccs7.cynexus.core.domain.valueobject.scheduler;

public record SchedulerCategoryUpdateCommand(String code,
                                             String label,
                                             String iconHint,
                                             Integer sortOrder,
                                             String description) {
}
