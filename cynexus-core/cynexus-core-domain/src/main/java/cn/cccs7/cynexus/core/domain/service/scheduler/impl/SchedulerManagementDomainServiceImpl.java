package cn.cccs7.cynexus.core.domain.service.scheduler.impl;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
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
}
