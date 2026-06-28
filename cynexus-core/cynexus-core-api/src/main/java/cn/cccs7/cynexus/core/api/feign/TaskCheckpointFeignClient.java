package cn.cccs7.cynexus.core.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.TaskCheckpointDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-core", contextId = "taskCheckpointFeignClient", path = "/api/core/task-checkpoint")
public interface TaskCheckpointFeignClient {

    @GetMapping
    Result<List<TaskCheckpointDTO>> list();

    @GetMapping("/{id}")
    Result<TaskCheckpointDTO> get(@PathVariable("id") Long id);

    @PostMapping
    Result<Long> create(@RequestBody TaskCheckpointDTO dto);

    @PutMapping
    Result<Boolean> update(@RequestBody TaskCheckpointDTO dto);

    @DeleteMapping("/{id}")
    Result<Boolean> delete(@PathVariable("id") Long id);
}
