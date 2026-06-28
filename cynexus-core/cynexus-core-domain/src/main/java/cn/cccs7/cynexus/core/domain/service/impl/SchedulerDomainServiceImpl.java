package cn.cccs7.cynexus.core.domain.service.impl;

import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.repository.SchedulerRepository;
import cn.cccs7.cynexus.core.domain.service.SchedulerDomainService;
import java.util.List;
import java.util.Map;

public class SchedulerDomainServiceImpl implements SchedulerDomainService {

    private final SchedulerRepository schedulerRepository;
    private final DataJobExecutorClient dataJobExecutorClient;

    public SchedulerDomainServiceImpl(SchedulerRepository schedulerRepository, DataJobExecutorClient dataJobExecutorClient) {
        this.schedulerRepository = schedulerRepository;
        this.dataJobExecutorClient = dataJobExecutorClient;
    }

    @Override
    public List<SchedulerJobBO> listJobs() {
        return schedulerRepository.listJobs();
    }

    @Override
    public SchedulerJobBO findJobById(Long id) {
        return schedulerRepository.findJobById(id);
    }

    @Override
    public Long createJob(SchedulerJobBO job) {
        return schedulerRepository.createJob(job);
    }

    @Override
    public boolean updateJob(SchedulerJobBO job) {
        return schedulerRepository.updateJob(job);
    }

    @Override
    public boolean deleteJob(Long id) {
        return schedulerRepository.deleteJob(id);
    }

    @Override
    public List<SchedulerJobCategoryBO> listCategories() {
        return schedulerRepository.listCategories();
    }

    @Override
    public SchedulerJobCategoryBO findCategoryById(Long id) {
        return schedulerRepository.findCategoryById(id);
    }

    @Override
    public Long createCategory(SchedulerJobCategoryBO category) {
        return schedulerRepository.createCategory(category);
    }

    @Override
    public boolean updateCategory(SchedulerJobCategoryBO category) {
        return schedulerRepository.updateCategory(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        return schedulerRepository.deleteCategory(id);
    }

    @Override
    public List<SchedulerJobCategoryMappingBO> listCategoryMappings() {
        return schedulerRepository.listCategoryMappings();
    }

    @Override
    public SchedulerJobCategoryMappingBO findCategoryMappingById(Long id) {
        return schedulerRepository.findCategoryMappingById(id);
    }

    @Override
    public Long createCategoryMapping(SchedulerJobCategoryMappingBO mapping) {
        return schedulerRepository.createCategoryMapping(mapping);
    }

    @Override
    public boolean updateCategoryMapping(SchedulerJobCategoryMappingBO mapping) {
        return schedulerRepository.updateCategoryMapping(mapping);
    }

    @Override
    public boolean deleteCategoryMapping(Long id) {
        return schedulerRepository.deleteCategoryMapping(id);
    }

    @Override
    public List<SchedulerJobRunHistoryBO> listRunHistory() {
        return schedulerRepository.listRunHistory();
    }

    @Override
    public SchedulerJobRunHistoryBO findRunHistoryById(Long id) {
        return schedulerRepository.findRunHistoryById(id);
    }

    @Override
    public Long createRunHistory(SchedulerJobRunHistoryBO runHistory) {
        return schedulerRepository.createRunHistory(runHistory);
    }

    @Override
    public boolean updateRunHistory(SchedulerJobRunHistoryBO runHistory) {
        return schedulerRepository.updateRunHistory(runHistory);
    }

    @Override
    public boolean deleteRunHistory(Long id) {
        return schedulerRepository.deleteRunHistory(id);
    }

    @Override
    public List<SchedulerJobStatusBO> listStatuses() {
        return schedulerRepository.listStatuses();
    }

    @Override
    public SchedulerJobStatusBO findStatusById(Long id) {
        return schedulerRepository.findStatusById(id);
    }

    @Override
    public Long createStatus(SchedulerJobStatusBO status) {
        return schedulerRepository.createStatus(status);
    }

    @Override
    public boolean updateStatus(SchedulerJobStatusBO status) {
        return schedulerRepository.updateStatus(status);
    }

    @Override
    public boolean deleteStatus(Long id) {
        return schedulerRepository.deleteStatus(id);
    }

    @Override
    public Map<String, Object> dataJobExecutorHealth() {
        return dataJobExecutorClient.health();
    }

    @Override
    public Map<String, Object> listDataJobs() {
        return dataJobExecutorClient.listJobs();
    }

    @Override
    public Map<String, Object> getDataJob(String jobKey) {
        return dataJobExecutorClient.getJob(jobKey);
    }

    @Override
    public DataJobTriggerResultBO triggerDataJob(String jobKey, DataJobTriggerRequestBO request) {
        return dataJobExecutorClient.triggerJob(jobKey, request);
    }

    @Override
    public Map<String, Object> getDataJobRun(String runId) {
        return dataJobExecutorClient.getJobRun(runId);
    }
}
