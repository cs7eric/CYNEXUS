package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.convert.SchedulerJobCategoryDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobCategoryMappingDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobRunHistoryDTOConverter;
import cn.cccs7.cynexus.core.application.convert.SchedulerJobStatusDTOConverter;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryMappingDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobRunHistoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobStatusDTO;
import cn.cccs7.cynexus.core.application.dto.DataJobTriggerResultDTO;
import cn.cccs7.cynexus.core.application.request.DataJobTriggerRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerJobCreateRequest;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryMappingBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.service.SchedulerDomainService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SchedulerAppService {

    private final SchedulerDomainService schedulerDomainService;

    public SchedulerAppService(SchedulerDomainService schedulerDomainService) {
        this.schedulerDomainService = schedulerDomainService;
    }

    public List<SchedulerJobDTO> listJobs() {
        return SchedulerJobDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listJobs());
    }

    public SchedulerJobDTO findJobById(Long id) {
        SchedulerJobBO job = schedulerDomainService.findJobById(id);
        return job == null ? null : SchedulerJobDTOConverter.INSTANCE.toSource(job);
    }

    public Long createJob(SchedulerJobCreateRequest request) {
        SchedulerJobDTO dto = SchedulerJobDTO.builder()
                .id(request.getId())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .serviceModule(request.getServiceModule())
                .serviceClass(request.getServiceClass())
                .configFile(request.getConfigFile())
                .enabled(request.getEnabled())
                .registeredAt(request.getRegisteredAt())
                .extra(request.getExtra())
                .remark(request.getRemark())
                .build();
        return schedulerDomainService.createJob(SchedulerJobDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateJob(SchedulerJobDTO dto) {
        return schedulerDomainService.updateJob(SchedulerJobDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteJob(Long id) {
        return schedulerDomainService.deleteJob(id);
    }

    public List<SchedulerJobCategoryDTO> listCategories() {
        return SchedulerJobCategoryDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listCategories());
    }

    public SchedulerJobCategoryDTO findCategoryById(Long id) {
        SchedulerJobCategoryBO category = schedulerDomainService.findCategoryById(id);
        return category == null ? null : SchedulerJobCategoryDTOConverter.INSTANCE.toSource(category);
    }

    public Long createCategory(SchedulerJobCategoryDTO dto) {
        return schedulerDomainService.createCategory(SchedulerJobCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategory(SchedulerJobCategoryDTO dto) {
        return schedulerDomainService.updateCategory(SchedulerJobCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategory(Long id) {
        return schedulerDomainService.deleteCategory(id);
    }

    public List<SchedulerJobCategoryMappingDTO> listCategoryMappings() {
        return SchedulerJobCategoryMappingDTOConverter.INSTANCE.toSourceList(
                schedulerDomainService.listCategoryMappings());
    }

    public SchedulerJobCategoryMappingDTO findCategoryMappingById(Long id) {
        SchedulerJobCategoryMappingBO mapping = schedulerDomainService.findCategoryMappingById(id);
        return mapping == null ? null : SchedulerJobCategoryMappingDTOConverter.INSTANCE.toSource(mapping);
    }

    public Long createCategoryMapping(SchedulerJobCategoryMappingDTO dto) {
        return schedulerDomainService.createCategoryMapping(
                SchedulerJobCategoryMappingDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategoryMapping(SchedulerJobCategoryMappingDTO dto) {
        return schedulerDomainService.updateCategoryMapping(
                SchedulerJobCategoryMappingDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategoryMapping(Long id) {
        return schedulerDomainService.deleteCategoryMapping(id);
    }

    public List<SchedulerJobRunHistoryDTO> listRunHistory() {
        return SchedulerJobRunHistoryDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listRunHistory());
    }

    public SchedulerJobRunHistoryDTO findRunHistoryById(Long id) {
        SchedulerJobRunHistoryBO runHistory = schedulerDomainService.findRunHistoryById(id);
        return runHistory == null ? null : SchedulerJobRunHistoryDTOConverter.INSTANCE.toSource(runHistory);
    }

    public Long createRunHistory(SchedulerJobRunHistoryDTO dto) {
        return schedulerDomainService.createRunHistory(SchedulerJobRunHistoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateRunHistory(SchedulerJobRunHistoryDTO dto) {
        return schedulerDomainService.updateRunHistory(SchedulerJobRunHistoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteRunHistory(Long id) {
        return schedulerDomainService.deleteRunHistory(id);
    }

    public List<SchedulerJobStatusDTO> listStatuses() {
        return SchedulerJobStatusDTOConverter.INSTANCE.toSourceList(schedulerDomainService.listStatuses());
    }

    public SchedulerJobStatusDTO findStatusById(Long id) {
        SchedulerJobStatusBO status = schedulerDomainService.findStatusById(id);
        return status == null ? null : SchedulerJobStatusDTOConverter.INSTANCE.toSource(status);
    }

    public Long createStatus(SchedulerJobStatusDTO dto) {
        return schedulerDomainService.createStatus(SchedulerJobStatusDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateStatus(SchedulerJobStatusDTO dto) {
        return schedulerDomainService.updateStatus(SchedulerJobStatusDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteStatus(Long id) {
        return schedulerDomainService.deleteStatus(id);
    }

    public Map<String, Object> dataJobExecutorHealth() {
        return schedulerDomainService.dataJobExecutorHealth();
    }

    public Map<String, Object> listDataJobs() {
        return schedulerDomainService.listDataJobs();
    }

    public Map<String, Object> getDataJob(String jobKey) {
        return schedulerDomainService.getDataJob(jobKey);
    }

    public DataJobTriggerResultDTO triggerDataJob(String jobKey, DataJobTriggerRequest request) {
        DataJobTriggerRequestBO bo = DataJobTriggerRequestBO.builder()
                .triggerRequestId(request == null ? null : request.getTriggerRequestId())
                .triggeredBy(request == null ? null : request.getTriggeredBy())
                .dryRun(request != null && Boolean.TRUE.equals(request.getDryRun()))
                .params(request == null || request.getParams() == null ? Map.of() : request.getParams())
                .build();
        DataJobTriggerResultBO result = schedulerDomainService.triggerDataJob(jobKey, bo);
        return DataJobTriggerResultDTO.builder()
                .accepted(result.getAccepted())
                .runId(result.getRunId())
                .jobKey(result.getJobKey())
                .status(result.getStatus())
                .result(result.getResult())
                .build();
    }

    public Map<String, Object> getDataJobRun(String runId) {
        return schedulerDomainService.getDataJobRun(runId);
    }
}
