package cn.cccs7.cynexus.core.domain.service.scheduler.impl;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerManagementPolicy;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryCreateCommand;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryDeleteOutcome;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryMutationResult;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerCategoryUpdateCommand;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SchedulerManagementDomainServiceImpl implements SchedulerManagementDomainService {

    private final SchedulerDomainService schedulerDomainService;

    public SchedulerManagementDomainServiceImpl(SchedulerDomainService schedulerDomainService) {
        this.schedulerDomainService = schedulerDomainService;
    }

    @Override
    public SchedulerJobBO requireJobByCode(String jobCode) {
        SchedulerJobBO job = schedulerDomainService.findJobByCode(jobCode);
        if (job == null || job.getDeletedAt() != null) {
            throw new IllegalArgumentException("Scheduler job not found: " + jobCode);
        }
        return job;
    }

    @Override
    public SchedulerJobStatusBO ensureStatus(SchedulerJobBO job) {
        Objects.requireNonNull(job, "job");
        SchedulerJobStatusBO status = schedulerDomainService.findStatusByJobId(job.getId());
        if (status != null && status.getDeletedAt() == null) {
            return status;
        }
        SchedulerJobStatusBO created = SchedulerJobStatusBO.builder()
                .jobId(job.getId())
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .running(false)
                .build();
        created.setId(schedulerDomainService.createStatus(created));
        return created;
    }

    @Override
    public SchedulerJobStatusBO setEnabled(SchedulerJobBO job, boolean enabled) {
        SchedulerJobStatusBO status = ensureStatus(job);
        if (!schedulerDomainService.updateJobEnabled(job.getId(), enabled)
                || !schedulerDomainService.updateStatusEnabled(status.getId(), enabled)) {
            throw new IllegalStateException("Failed to update scheduler job enabled state: " + job.getCode());
        }
        job.setEnabled(enabled);
        status.setEnabled(enabled);
        return status;
    }

    @Override
    public SchedulerCategoryMutationResult createCategory(SchedulerCategoryCreateCommand command) {
        if (command == null || command.label() == null || command.label().isBlank()) {
            throw new IllegalArgumentException("Scheduler category label is required");
        }
        List<SchedulerJobCategoryBO> existingCategories = activeCategories();
        String label = command.label().trim();
        String code = SchedulerManagementPolicy.uniqueCategoryCode(
                firstNonBlank(command.code(), SchedulerManagementPolicy.categoryCode(label)), existingCategories);
        String iconHint = firstNonBlank(command.iconHint(), "grid");
        int sortOrder = command.sortOrder() == null
                ? SchedulerManagementPolicy.nextCategorySortOrder(existingCategories)
                : command.sortOrder();
        Long id = schedulerDomainService.createCategory(SchedulerJobCategoryBO.builder()
                .code(code)
                .label(label)
                .iconHint(iconHint)
                .sortOrder(sortOrder)
                .description(SchedulerManagementPolicy.blankToNull(command.description()))
                .status("active")
                .remark("scheduler-management")
                .build());
        return new SchedulerCategoryMutationResult(schedulerDomainService.findCategoryById(id), 0);
    }

    @Override
    public SchedulerCategoryMutationResult updateCategory(Long categoryId, SchedulerCategoryUpdateCommand command) {
        SchedulerJobCategoryBO category = schedulerDomainService.findCategoryById(categoryId);
        if (category == null || category.getDeletedAt() != null) {
            throw new IllegalArgumentException("Scheduler category not found: " + categoryId);
        }
        if (command == null || command.label() == null || command.label().isBlank()) {
            throw new IllegalArgumentException("Scheduler category label is required");
        }
        List<SchedulerJobCategoryBO> otherCategories = activeCategories().stream()
                .filter(item -> !Objects.equals(item.getId(), categoryId))
                .toList();
        String label = command.label().trim();
        String code = SchedulerManagementPolicy.uniqueCategoryCode(
                firstNonBlank(command.code(), firstNonBlank(category.getCode(), SchedulerManagementPolicy.categoryCode(label))),
                otherCategories);
        String iconHint = firstNonBlank(command.iconHint(), firstNonBlank(category.getIconHint(), "grid"));
        Integer sortOrder = command.sortOrder() == null ? category.getSortOrder() : command.sortOrder();
        boolean updated = schedulerDomainService.updateCategory(SchedulerJobCategoryBO.builder()
                .id(category.getId())
                .code(code)
                .label(label)
                .iconHint(iconHint)
                .sortOrder(sortOrder)
                .description(command.description() == null ? category.getDescription() : command.description().trim())
                .status(firstNonBlank(category.getStatus(), "active"))
                .remark(firstNonBlank(category.getRemark(), "scheduler-management"))
                .build());
        if (!updated) {
            throw new IllegalStateException("Scheduler category update failed: " + categoryId);
        }
        return new SchedulerCategoryMutationResult(
                schedulerDomainService.findCategoryById(categoryId), activeMappingCount(categoryId));
    }

    @Override
    public SchedulerCategoryDeleteOutcome deleteCategory(Long categoryId) {
        SchedulerJobCategoryBO category = schedulerDomainService.findCategoryById(categoryId);
        if (category == null || category.getDeletedAt() != null) {
            throw new IllegalArgumentException("Scheduler category not found: " + categoryId);
        }
        List<SchedulerJobCategoryMappingBO> mappings = schedulerDomainService.listCategoryMappings().stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .filter(mapping -> Objects.equals(mapping.getCategoryId(), categoryId))
                .toList();
        mappings.forEach(mapping -> schedulerDomainService.deleteCategoryMapping(mapping.getId()));
        return new SchedulerCategoryDeleteOutcome(schedulerDomainService.deleteCategory(categoryId), mappings.size());
    }

    @Override
    public boolean deleteJob(SchedulerJobBO job) {
        Objects.requireNonNull(job, "job");
        List<SchedulerJobCategoryMappingBO> mappings = schedulerDomainService
                .listCategoryMappingsByJobId(job.getId());
        for (SchedulerJobCategoryMappingBO mapping : mappings) {
            if (mapping.getDeletedAt() == null
                    && !schedulerDomainService.deleteCategoryMapping(mapping.getId())) {
                return false;
            }
        }
        SchedulerJobStatusBO status = schedulerDomainService.findStatusByJobId(job.getId());
        if (status != null && status.getDeletedAt() == null
                && !schedulerDomainService.deleteStatus(status.getId())) {
            return false;
        }
        return schedulerDomainService.deleteJob(job.getId());
    }

    @Override
    public List<SchedulerJobCategoryMappingBO> replaceCategoryMappings(
            SchedulerJobBO job, Set<Long> requestedCategoryIds) {
        Objects.requireNonNull(job, "job");
        Set<Long> requested = requestedCategoryIds == null ? Set.of() : requestedCategoryIds;
        Map<Long, Boolean> activeCategories = new HashMap<>();
        schedulerDomainService.listCategories().stream()
                .filter(category -> category.getDeletedAt() == null)
                .forEach(category -> activeCategories.put(category.getId(), true));
        for (Long categoryId : requested) {
            if (!activeCategories.containsKey(categoryId)) {
                throw new IllegalArgumentException("Scheduler category not found: " + categoryId);
            }
        }

        List<SchedulerJobCategoryMappingBO> allMappings = schedulerDomainService.listCategoryMappings();
        Map<Long, Integer> maxSortOrderByCategoryId = new HashMap<>();
        for (SchedulerJobCategoryMappingBO mapping : allMappings) {
            if (mapping.getDeletedAt() == null && mapping.getCategoryId() != null) {
                maxSortOrderByCategoryId.merge(mapping.getCategoryId(),
                        mapping.getSortOrder() == null ? 0 : mapping.getSortOrder(), Math::max);
            }
        }
        Map<Long, SchedulerJobCategoryMappingBO> keptByCategoryId = new LinkedHashMap<>();
        for (SchedulerJobCategoryMappingBO mapping : schedulerDomainService.listCategoryMappingsByJobId(job.getId())) {
            Long categoryId = mapping.getCategoryId();
            boolean keep = mapping.getDeletedAt() == null
                    && categoryId != null
                    && requested.contains(categoryId)
                    && !keptByCategoryId.containsKey(categoryId);
            if (keep) {
                keptByCategoryId.put(categoryId, mapping);
            } else if (mapping.getDeletedAt() == null) {
                schedulerDomainService.deleteCategoryMapping(mapping.getId());
            }
        }
        for (Long categoryId : requested) {
            if (keptByCategoryId.containsKey(categoryId)) {
                continue;
            }
            int sortOrder = maxSortOrderByCategoryId.getOrDefault(categoryId, 0) + 10;
            maxSortOrderByCategoryId.put(categoryId, sortOrder);
            schedulerDomainService.createCategoryMapping(SchedulerJobCategoryMappingBO.builder()
                    .jobId(job.getId())
                    .categoryId(categoryId)
                    .sortOrder(sortOrder)
                    .remark("scheduler-management")
                    .build());
        }
        return schedulerDomainService.listCategoryMappingsByJobId(job.getId()).stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .sorted(Comparator.comparing(SchedulerJobCategoryMappingBO::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<SchedulerJobCategoryBO> activeCategories() {
        return schedulerDomainService.listCategories().stream()
                .filter(category -> category.getDeletedAt() == null)
                .toList();
    }

    private int activeMappingCount(Long categoryId) {
        return (int) schedulerDomainService.listCategoryMappings().stream()
                .filter(mapping -> mapping.getDeletedAt() == null)
                .filter(mapping -> Objects.equals(mapping.getCategoryId(), categoryId))
                .count();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
