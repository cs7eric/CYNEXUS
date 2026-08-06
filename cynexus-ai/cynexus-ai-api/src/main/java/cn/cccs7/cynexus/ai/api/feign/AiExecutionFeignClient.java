package cn.cccs7.cynexus.ai.api.feign;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cynexus-ai", contextId = "aiExecutionFeignClient", path = "/api/ai/executions")
public interface AiExecutionFeignClient {

    @PostMapping("/vision")
    Result<AiExecuteDTO> executeVision(@RequestBody AiVisionExecuteRequest request);
}
