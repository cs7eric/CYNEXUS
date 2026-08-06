package cn.cccs7.cynexus.core.domain.scheduler.impl;

import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.repository.SchedulerRepository;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    public SchedulerJobBO findJobByCode(String code) {
        return schedulerRepository.findJobByCode(code);
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
    public boolean updateJobEnabled(Long id, Boolean enabled) {
        return schedulerRepository.updateJobEnabled(id, enabled);
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
    public List<SchedulerJobCategoryMappingBO> listCategoryMappingsByJobId(Long jobId) {
        return schedulerRepository.listCategoryMappingsByJobId(jobId);
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
    public List<SchedulerJobRunHistoryBO> listRunHistoryByJobId(Long jobId, Integer limit) {
        return schedulerRepository.listRunHistoryByJobId(jobId, limit);
    }

    @Override
    public List<SchedulerJobRunHistoryBO> listRunHistorySince(OffsetDateTime startedAt) {
        return schedulerRepository.listRunHistorySince(startedAt);
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
    public SchedulerJobStatusBO findStatusByJobId(Long jobId) {
        return schedulerRepository.findStatusByJobId(jobId);
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
    public boolean updateStatusEnabled(Long id, Boolean enabled) {
        return schedulerRepository.updateStatusEnabled(id, enabled);
    }

    @Override
    public boolean updateStatusRunning(Long id,
                                       Boolean running,
                                       OffsetDateTime schedulerStartedAt,
                                       OffsetDateTime stoppedAt) {
        return schedulerRepository.updateStatusRunning(id, running, schedulerStartedAt, stoppedAt);
    }

    @Override
    public boolean updateStatusRunResult(Long id,
                                         OffsetDateTime lastRunAt,
                                         Boolean lastRunOk,
                                         String lastRunStatus,
                                         String lastRunErrorMessage,
                                         BigDecimal lastDurationSeconds) {
        return schedulerRepository.updateStatusRunResult(
                id,
                lastRunAt,
                lastRunOk,
                lastRunStatus,
                lastRunErrorMessage,
                lastDurationSeconds);
    }

    @Override
    public boolean updateStatusRunResult(Long id,
                                         OffsetDateTime lastRunAt,
                                         Boolean lastRunOk,
                                         String lastRunStatus,
                                         String lastRunErrorMessage,
                                         BigDecimal lastDurationSeconds,
                                         Integer lastTargetsProcessed,
                                         String progress,
                                         String extra,
                                         String remark) {
        return schedulerRepository.updateStatusRunResult(
                id,
                lastRunAt,
                lastRunOk,
                lastRunStatus,
                lastRunErrorMessage,
                lastDurationSeconds,
                lastTargetsProcessed,
                progress,
                extra,
                remark);
    }

    @Override
    public boolean updateStatusProgress(Long id,
                                        Boolean running,
                                        OffsetDateTime lastRunAt,
                                        String lastRunStatus,
                                        String progress,
                                        String extra,
                                        String remark) {
        return schedulerRepository.updateStatusProgress(id, running, lastRunAt, lastRunStatus, progress, extra, remark);
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

    @Override
    public Map<String, Object> startSocialLogin(String platform, String profileName) {
        return dataJobExecutorClient.startSocialLogin(platform, profileName);
    }

    @Override
    public Map<String, Object> getSocialLogin(String sessionId) {
        return dataJobExecutorClient.getSocialLogin(sessionId);
    }

    @Override
    public Map<String, Object> completeSocialLogin(String sessionId) {
        return dataJobExecutorClient.completeSocialLogin(sessionId);
    }

    @Override
    public Map<String, Object> closeSocialLogin(String sessionId) {
        return dataJobExecutorClient.closeSocialLogin(sessionId);
    }
}
