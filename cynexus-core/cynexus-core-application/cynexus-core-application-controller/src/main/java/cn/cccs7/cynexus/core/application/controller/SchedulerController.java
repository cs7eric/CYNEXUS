package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryMappingDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobRunHistoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobStatusDTO;
import cn.cccs7.cynexus.core.application.dto.DataJobTriggerResultDTO;
import cn.cccs7.cynexus.core.application.request.DataJobTriggerRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerJobCreateRequest;
import cn.cccs7.cynexus.core.application.service.SchedulerAppService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/scheduler")
public class SchedulerController {

    private final SchedulerAppService schedulerAppService;

    public SchedulerController(SchedulerAppService schedulerAppService) {
        this.schedulerAppService = schedulerAppService;
    }

    @GetMapping("/jobs")
    public Result<List<SchedulerJobDTO>> listJobs() {
        return Result.success(schedulerAppService.listJobs());
    }

    @GetMapping("/jobs/{id}")
    public Result<SchedulerJobDTO> getJob(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.findJobById(id));
    }

    @PostMapping("/jobs")
    public Result<Long> createJob(@RequestBody SchedulerJobCreateRequest request) {
        return Result.success(schedulerAppService.createJob(request));
    }

    @PutMapping("/jobs")
    public Result<Boolean> updateJob(@RequestBody SchedulerJobDTO dto) {
        return Result.success(schedulerAppService.updateJob(dto));
    }

    @DeleteMapping("/jobs/{id}")
    public Result<Boolean> deleteJob(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.deleteJob(id));
    }

    @GetMapping("/categories")
    public Result<List<SchedulerJobCategoryDTO>> listCategories() {
        return Result.success(schedulerAppService.listCategories());
    }

    @GetMapping("/categories/{id}")
    public Result<SchedulerJobCategoryDTO> getCategory(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.findCategoryById(id));
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody SchedulerJobCategoryDTO dto) {
        return Result.success(schedulerAppService.createCategory(dto));
    }

    @PutMapping("/categories")
    public Result<Boolean> updateCategory(@RequestBody SchedulerJobCategoryDTO dto) {
        return Result.success(schedulerAppService.updateCategory(dto));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Boolean> deleteCategory(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.deleteCategory(id));
    }

    @GetMapping("/category-mappings")
    public Result<List<SchedulerJobCategoryMappingDTO>> listCategoryMappings() {
        return Result.success(schedulerAppService.listCategoryMappings());
    }

    @GetMapping("/category-mappings/{id}")
    public Result<SchedulerJobCategoryMappingDTO> getCategoryMapping(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.findCategoryMappingById(id));
    }

    @PostMapping("/category-mappings")
    public Result<Long> createCategoryMapping(@RequestBody SchedulerJobCategoryMappingDTO dto) {
        return Result.success(schedulerAppService.createCategoryMapping(dto));
    }

    @PutMapping("/category-mappings")
    public Result<Boolean> updateCategoryMapping(@RequestBody SchedulerJobCategoryMappingDTO dto) {
        return Result.success(schedulerAppService.updateCategoryMapping(dto));
    }

    @DeleteMapping("/category-mappings/{id}")
    public Result<Boolean> deleteCategoryMapping(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.deleteCategoryMapping(id));
    }

    @GetMapping("/run-history")
    public Result<List<SchedulerJobRunHistoryDTO>> listRunHistory() {
        return Result.success(schedulerAppService.listRunHistory());
    }

    @GetMapping("/run-history/{id}")
    public Result<SchedulerJobRunHistoryDTO> getRunHistory(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.findRunHistoryById(id));
    }

    @PostMapping("/run-history")
    public Result<Long> createRunHistory(@RequestBody SchedulerJobRunHistoryDTO dto) {
        return Result.success(schedulerAppService.createRunHistory(dto));
    }

    @PutMapping("/run-history")
    public Result<Boolean> updateRunHistory(@RequestBody SchedulerJobRunHistoryDTO dto) {
        return Result.success(schedulerAppService.updateRunHistory(dto));
    }

    @DeleteMapping("/run-history/{id}")
    public Result<Boolean> deleteRunHistory(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.deleteRunHistory(id));
    }

    @GetMapping("/statuses")
    public Result<List<SchedulerJobStatusDTO>> listStatuses() {
        return Result.success(schedulerAppService.listStatuses());
    }

    @GetMapping("/statuses/{id}")
    public Result<SchedulerJobStatusDTO> getStatus(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.findStatusById(id));
    }

    @PostMapping("/statuses")
    public Result<Long> createStatus(@RequestBody SchedulerJobStatusDTO dto) {
        return Result.success(schedulerAppService.createStatus(dto));
    }

    @PutMapping("/statuses")
    public Result<Boolean> updateStatus(@RequestBody SchedulerJobStatusDTO dto) {
        return Result.success(schedulerAppService.updateStatus(dto));
    }

    @DeleteMapping("/statuses/{id}")
    public Result<Boolean> deleteStatus(@PathVariable("id") Long id) {
        return Result.success(schedulerAppService.deleteStatus(id));
    }

    @GetMapping("/data-jobs/health")
    public Result<Map<String, Object>> dataJobExecutorHealth() {
        return Result.success(schedulerAppService.dataJobExecutorHealth());
    }

    @GetMapping("/data-jobs")
    public Result<Map<String, Object>> listDataJobs() {
        return Result.success(schedulerAppService.listDataJobs());
    }

    @GetMapping("/data-jobs/{jobKey}")
    public Result<Map<String, Object>> getDataJob(@PathVariable("jobKey") String jobKey) {
        return Result.success(schedulerAppService.getDataJob(jobKey));
    }

    @PostMapping("/data-jobs/{jobKey}/trigger")
    public Result<DataJobTriggerResultDTO> triggerDataJob(@PathVariable("jobKey") String jobKey,
                                                         @RequestBody DataJobTriggerRequest request) {
        return Result.success(schedulerAppService.triggerDataJob(jobKey, request));
    }

    @GetMapping("/data-job-runs/{runId}")
    public Result<Map<String, Object>> getDataJobRun(@PathVariable("runId") String runId) {
        return Result.success(schedulerAppService.getDataJobRun(runId));
    }

    @PostMapping("/social/login-sessions")
    public Result<Map<String, Object>> startSocialLogin(@RequestBody Map<String, Object> request) {
        String platform = request == null ? null : String.valueOf(request.getOrDefault("platform", ""));
        String profileName = request == null ? "default" : String.valueOf(request.getOrDefault("profile_name", "default"));
        return Result.success(schedulerAppService.startSocialLogin(platform, profileName));
    }

    @GetMapping("/social/login-sessions/{sessionId}")
    public Result<Map<String, Object>> getSocialLogin(@PathVariable("sessionId") String sessionId) {
        return Result.success(schedulerAppService.getSocialLogin(sessionId));
    }

    @PostMapping("/social/login-sessions/{sessionId}/complete")
    public Result<Map<String, Object>> completeSocialLogin(@PathVariable("sessionId") String sessionId) {
        return Result.success(schedulerAppService.completeSocialLogin(sessionId));
    }

    @DeleteMapping("/social/login-sessions/{sessionId}")
    public Result<Map<String, Object>> closeSocialLogin(@PathVariable("sessionId") String sessionId) {
        return Result.success(schedulerAppService.closeSocialLogin(sessionId));
    }
}
