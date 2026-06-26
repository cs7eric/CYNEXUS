package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobCategoryMappingDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobRunHistoryDTO;
import cn.cccs7.cynexus.core.application.dto.SchedulerJobStatusDTO;
import cn.cccs7.cynexus.core.application.request.SchedulerJobCreateRequest;
import cn.cccs7.cynexus.core.application.service.SchedulerAppService;
import java.util.List;
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
    public Result<SchedulerJobDTO> getJob(@PathVariable Long id) {
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
    public Result<Boolean> deleteJob(@PathVariable Long id) {
        return Result.success(schedulerAppService.deleteJob(id));
    }

    @GetMapping("/categories")
    public Result<List<SchedulerJobCategoryDTO>> listCategories() {
        return Result.success(schedulerAppService.listCategories());
    }

    @GetMapping("/categories/{id}")
    public Result<SchedulerJobCategoryDTO> getCategory(@PathVariable Long id) {
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
    public Result<Boolean> deleteCategory(@PathVariable Long id) {
        return Result.success(schedulerAppService.deleteCategory(id));
    }

    @GetMapping("/category-mappings")
    public Result<List<SchedulerJobCategoryMappingDTO>> listCategoryMappings() {
        return Result.success(schedulerAppService.listCategoryMappings());
    }

    @GetMapping("/category-mappings/{id}")
    public Result<SchedulerJobCategoryMappingDTO> getCategoryMapping(@PathVariable Long id) {
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
    public Result<Boolean> deleteCategoryMapping(@PathVariable Long id) {
        return Result.success(schedulerAppService.deleteCategoryMapping(id));
    }

    @GetMapping("/run-history")
    public Result<List<SchedulerJobRunHistoryDTO>> listRunHistory() {
        return Result.success(schedulerAppService.listRunHistory());
    }

    @GetMapping("/run-history/{id}")
    public Result<SchedulerJobRunHistoryDTO> getRunHistory(@PathVariable Long id) {
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
    public Result<Boolean> deleteRunHistory(@PathVariable Long id) {
        return Result.success(schedulerAppService.deleteRunHistory(id));
    }

    @GetMapping("/statuses")
    public Result<List<SchedulerJobStatusDTO>> listStatuses() {
        return Result.success(schedulerAppService.listStatuses());
    }

    @GetMapping("/statuses/{id}")
    public Result<SchedulerJobStatusDTO> getStatus(@PathVariable Long id) {
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
    public Result<Boolean> deleteStatus(@PathVariable Long id) {
        return Result.success(schedulerAppService.deleteStatus(id));
    }
}
