package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementCategoriesResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementDailyStatsResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobActionResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobHistoryResponseDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobsResponseDTO;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementCategoryCreateRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementCategoryUpdateRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementJobCategoriesRequest;
import cn.cccs7.cynexus.core.application.request.SchedulerManagementTriggerRequest;
import cn.cccs7.cynexus.core.application.service.SchedulerAppService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/scheduler/management")
public class SchedulerManagementController {

    private final SchedulerAppService schedulerAppService;

    public SchedulerManagementController(SchedulerAppService schedulerAppService) {
        this.schedulerAppService = schedulerAppService;
    }

    @GetMapping("/jobs")
    public Result<SchedulerManagementJobsResponseDTO> listJobs() {
        return Result.success(schedulerAppService.listManagementJobs());
    }

    @GetMapping("/categories")
    public Result<SchedulerManagementCategoriesResponseDTO> listCategories() {
        return Result.success(schedulerAppService.listManagementCategories());
    }

    @PostMapping("/categories")
    public Result<SchedulerManagementCategoriesResponseDTO.Item> createCategory(
            @RequestBody SchedulerManagementCategoryCreateRequest request) {
        return Result.success(schedulerAppService.createManagementCategory(request));
    }

    @PutMapping("/categories/{categoryId}")
    public Result<SchedulerManagementCategoriesResponseDTO.Item> updateCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestBody SchedulerManagementCategoryUpdateRequest request) {
        return Result.success(schedulerAppService.updateManagementCategory(categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public Result<SchedulerManagementJobActionResponseDTO> deleteCategory(
            @PathVariable("categoryId") Long categoryId) {
        return Result.success(schedulerAppService.deleteManagementCategory(categoryId));
    }

    @GetMapping("/stats/daily")
    public Result<SchedulerManagementDailyStatsResponseDTO> listDailyStats(
            @RequestParam(value = "days", defaultValue = "14") Integer days) {
        return Result.success(schedulerAppService.listManagementDailyStats(days == null ? 14 : days));
    }

    @GetMapping("/jobs/{jobCode}/history")
    public Result<SchedulerManagementJobHistoryResponseDTO> listJobHistory(
            @PathVariable("jobCode") String jobCode,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        return Result.success(schedulerAppService.listManagementJobHistory(jobCode, limit == null ? 50 : limit));
    }

    @PostMapping("/jobs/{jobCode}/categories")
    public Result<SchedulerManagementJobActionResponseDTO> updateJobCategories(
            @PathVariable("jobCode") String jobCode,
            @RequestBody SchedulerManagementJobCategoriesRequest request) {
        return Result.success(schedulerAppService.updateManagementJobCategories(jobCode, request));
    }

    @PostMapping("/jobs/{jobCode}/enable")
    public Result<SchedulerManagementJobActionResponseDTO> enableJob(@PathVariable("jobCode") String jobCode) {
        return Result.success(schedulerAppService.enableManagementJob(jobCode));
    }

    @PostMapping("/jobs/{jobCode}/disable")
    public Result<SchedulerManagementJobActionResponseDTO> disableJob(@PathVariable("jobCode") String jobCode) {
        return Result.success(schedulerAppService.disableManagementJob(jobCode));
    }

    @PostMapping("/jobs/{jobCode}/start")
    public Result<SchedulerManagementJobActionResponseDTO> startJob(@PathVariable("jobCode") String jobCode) {
        return Result.success(schedulerAppService.startManagementJob(jobCode));
    }

    @PostMapping("/jobs/{jobCode}/stop")
    public Result<SchedulerManagementJobActionResponseDTO> stopJob(@PathVariable("jobCode") String jobCode) {
        return Result.success(schedulerAppService.stopManagementJob(jobCode));
    }

    @PostMapping("/jobs/{jobCode}/trigger")
    public Result<SchedulerManagementJobActionResponseDTO> triggerJob(
            @PathVariable("jobCode") String jobCode,
            @RequestBody(required = false) SchedulerManagementTriggerRequest request) {
        return Result.success(schedulerAppService.triggerManagementJob(jobCode, request));
    }

    @DeleteMapping("/jobs/{jobCode}")
    public Result<SchedulerManagementJobActionResponseDTO> deleteJob(@PathVariable("jobCode") String jobCode) {
        return Result.success(schedulerAppService.deleteManagementJob(jobCode));
    }
}
