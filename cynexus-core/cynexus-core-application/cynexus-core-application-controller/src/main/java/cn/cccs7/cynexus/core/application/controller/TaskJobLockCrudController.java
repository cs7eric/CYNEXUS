package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.TaskJobLockDTO;
import cn.cccs7.cynexus.core.application.service.TaskJobLockCrudAppService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/task-job-lock")
public class TaskJobLockCrudController {

    private final TaskJobLockCrudAppService appService;

    public TaskJobLockCrudController(TaskJobLockCrudAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<List<TaskJobLockDTO>> list() {
        return Result.success(appService.list());
    }

    @GetMapping("/{id}")
    public Result<TaskJobLockDTO> get(@PathVariable("id") Long id) {
        return Result.success(appService.findById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody TaskJobLockDTO dto) {
        return Result.success(appService.create(dto));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody TaskJobLockDTO dto) {
        return Result.success(appService.update(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(appService.delete(id));
    }
}
