package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.repository.SchedulerRepository;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryMappingBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobCategoryBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobRunHistoryBasicService;
import cn.cccs7.cynexus.core.infra.basic.service.SchedulerJobStatusBasicService;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobCategoryMappingPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobCategoryPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobRunHistoryPOConverter;
import cn.cccs7.cynexus.core.infra.convert.SchedulerJobStatusPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SchedulerRepositoryImpl implements SchedulerRepository {

    private final SchedulerJobBasicService schedulerJobBasicService;
    private final SchedulerJobCategoryBasicService schedulerJobCategoryBasicService;
    private final SchedulerJobCategoryMappingBasicService schedulerJobCategoryMappingBasicService;
    private final SchedulerJobRunHistoryBasicService schedulerJobRunHistoryBasicService;
    private final SchedulerJobStatusBasicService schedulerJobStatusBasicService;
    public SchedulerRepositoryImpl(SchedulerJobBasicService schedulerJobBasicService,
                                   SchedulerJobCategoryBasicService schedulerJobCategoryBasicService,
                                   SchedulerJobCategoryMappingBasicService schedulerJobCategoryMappingBasicService,
                                   SchedulerJobRunHistoryBasicService schedulerJobRunHistoryBasicService,
                                   SchedulerJobStatusBasicService schedulerJobStatusBasicService) {
        this.schedulerJobBasicService = schedulerJobBasicService;
        this.schedulerJobCategoryBasicService = schedulerJobCategoryBasicService;
        this.schedulerJobCategoryMappingBasicService = schedulerJobCategoryMappingBasicService;
        this.schedulerJobRunHistoryBasicService = schedulerJobRunHistoryBasicService;
        this.schedulerJobStatusBasicService = schedulerJobStatusBasicService;
    }

    @Override
    public List<SchedulerJobBO> listJobs() {
        return SchedulerJobPOConverter.INSTANCE.toTargetList(schedulerJobBasicService.listAll());
    }

    @Override
    public SchedulerJobBO findJobById(Long id) {
        return Optional.ofNullable(schedulerJobBasicService.getById(id))
                .map(SchedulerJobPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long createJob(SchedulerJobBO job) {
        return schedulerJobBasicService.create(SchedulerJobPOConverter.INSTANCE.toSource(job));
    }

    @Override
    public boolean updateJob(SchedulerJobBO job) {
        return schedulerJobBasicService.update(SchedulerJobPOConverter.INSTANCE.toSource(job));
    }

    @Override
    public boolean deleteJob(Long id) {
        return schedulerJobBasicService.delete(id);
    }

    @Override
    public List<SchedulerJobCategoryBO> listCategories() {
        return SchedulerJobCategoryPOConverter.INSTANCE.toTargetList(schedulerJobCategoryBasicService.listAll());
    }

    @Override
    public SchedulerJobCategoryBO findCategoryById(Long id) {
        return Optional.ofNullable(schedulerJobCategoryBasicService.getById(id))
                .map(SchedulerJobCategoryPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long createCategory(SchedulerJobCategoryBO category) {
        return schedulerJobCategoryBasicService.create(SchedulerJobCategoryPOConverter.INSTANCE.toSource(category));
    }

    @Override
    public boolean updateCategory(SchedulerJobCategoryBO category) {
        return schedulerJobCategoryBasicService.update(SchedulerJobCategoryPOConverter.INSTANCE.toSource(category));
    }

    @Override
    public boolean deleteCategory(Long id) {
        return schedulerJobCategoryBasicService.delete(id);
    }

    @Override
    public List<SchedulerJobCategoryMappingBO> listCategoryMappings() {
        return SchedulerJobCategoryMappingPOConverter.INSTANCE.toTargetList(schedulerJobCategoryMappingBasicService.listAll());
    }

    @Override
    public SchedulerJobCategoryMappingBO findCategoryMappingById(Long id) {
        return Optional.ofNullable(schedulerJobCategoryMappingBasicService.getById(id))
                .map(SchedulerJobCategoryMappingPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long createCategoryMapping(SchedulerJobCategoryMappingBO mapping) {
        return schedulerJobCategoryMappingBasicService.create(SchedulerJobCategoryMappingPOConverter.INSTANCE.toSource(mapping));
    }

    @Override
    public boolean updateCategoryMapping(SchedulerJobCategoryMappingBO mapping) {
        return schedulerJobCategoryMappingBasicService.update(SchedulerJobCategoryMappingPOConverter.INSTANCE.toSource(mapping));
    }

    @Override
    public boolean deleteCategoryMapping(Long id) {
        return schedulerJobCategoryMappingBasicService.delete(id);
    }

    @Override
    public List<SchedulerJobRunHistoryBO> listRunHistory() {
        return SchedulerJobRunHistoryPOConverter.INSTANCE.toTargetList(schedulerJobRunHistoryBasicService.listAll());
    }

    @Override
    public SchedulerJobRunHistoryBO findRunHistoryById(Long id) {
        return Optional.ofNullable(schedulerJobRunHistoryBasicService.getById(id))
                .map(SchedulerJobRunHistoryPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long createRunHistory(SchedulerJobRunHistoryBO runHistory) {
        return schedulerJobRunHistoryBasicService.create(SchedulerJobRunHistoryPOConverter.INSTANCE.toSource(runHistory));
    }

    @Override
    public boolean updateRunHistory(SchedulerJobRunHistoryBO runHistory) {
        return schedulerJobRunHistoryBasicService.update(SchedulerJobRunHistoryPOConverter.INSTANCE.toSource(runHistory));
    }

    @Override
    public boolean deleteRunHistory(Long id) {
        return schedulerJobRunHistoryBasicService.delete(id);
    }

    @Override
    public List<SchedulerJobStatusBO> listStatuses() {
        return SchedulerJobStatusPOConverter.INSTANCE.toTargetList(schedulerJobStatusBasicService.listAll());
    }

    @Override
    public SchedulerJobStatusBO findStatusById(Long id) {
        return Optional.ofNullable(schedulerJobStatusBasicService.getById(id))
                .map(SchedulerJobStatusPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long createStatus(SchedulerJobStatusBO status) {
        return schedulerJobStatusBasicService.create(SchedulerJobStatusPOConverter.INSTANCE.toSource(status));
    }

    @Override
    public boolean updateStatus(SchedulerJobStatusBO status) {
        return schedulerJobStatusBasicService.update(SchedulerJobStatusPOConverter.INSTANCE.toSource(status));
    }

    @Override
    public boolean deleteStatus(Long id) {
        return schedulerJobStatusBasicService.delete(id);
    }
}
