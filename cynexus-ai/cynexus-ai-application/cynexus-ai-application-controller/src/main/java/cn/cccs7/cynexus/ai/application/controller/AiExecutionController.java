package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.application.service.ScenarioExecutionAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/executions")
public class AiExecutionController {

    private final ScenarioExecutionAppService scenarioExecutionAppService;

    public AiExecutionController(ScenarioExecutionAppService scenarioExecutionAppService) {
        this.scenarioExecutionAppService = scenarioExecutionAppService;
    }

    @PostMapping
    public Result<AiExecuteDTO> execute(@RequestBody AiExecuteRequest request) {
        return Result.success(scenarioExecutionAppService.execute(request));
    }

    @PostMapping("/vision")
    public Result<AiExecuteDTO> executeVision(@RequestBody AiVisionExecuteRequest request) {
        return Result.success(scenarioExecutionAppService.executeVision(request));
    }
}
