package cn.cccs7.cynexus.core.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.TaskJobLockDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-core", contextId = "taskJobLockFeignClient", path = "/api/core/task-job-lock")
public interface TaskJobLockFeignClient {

    @GetMapping
    Result<List<TaskJobLockDTO>> list();

    @GetMapping("/{id}")
    Result<TaskJobLockDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody TaskJobLockDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody TaskJobLockDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
