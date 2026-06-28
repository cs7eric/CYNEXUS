package cn.cccs7.cynexus.core.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.TaskJobRunDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-core", contextId = "taskJobRunFeignClient", path = "/api/core/task-job-run")
public interface TaskJobRunFeignClient {

    @GetMapping
    Result<List<TaskJobRunDTO>> list();

    @GetMapping("/{id}")
    Result<TaskJobRunDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody TaskJobRunDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody TaskJobRunDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
