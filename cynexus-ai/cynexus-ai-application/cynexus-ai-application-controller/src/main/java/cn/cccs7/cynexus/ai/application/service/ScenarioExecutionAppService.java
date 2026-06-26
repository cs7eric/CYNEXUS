package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.application.assembler.AiExecutionAssembler;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import cn.cccs7.cynexus.ai.domain.service.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.infra.observability.AiTraceLogger;
import cn.cccs7.cynexus.ai.infra.tool.registry.AiToolRegistry;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ScenarioExecutionAppService {

    private final ModelRouteAppService modelRouteAppService;
    private final PromptRenderAppService promptRenderAppService;
    private final AgentExecutor agentExecutor;
    private final AiToolRegistry aiToolRegistry;
    private final AiTraceLogger aiTraceLogger;
    private final OutputValidationDomainService outputValidationDomainService;
    private final AiExecutionAssembler aiExecutionAssembler;

    public ScenarioExecutionAppService(ModelRouteAppService modelRouteAppService,
                                       PromptRenderAppService promptRenderAppService,
                                       AgentExecutor agentExecutor,
                                       AiToolRegistry aiToolRegistry,
                                       AiTraceLogger aiTraceLogger,
                                       OutputValidationDomainService outputValidationDomainService,
                                       AiExecutionAssembler aiExecutionAssembler) {
        this.modelRouteAppService = modelRouteAppService;
        this.promptRenderAppService = promptRenderAppService;
        this.agentExecutor = agentExecutor;
        this.aiToolRegistry = aiToolRegistry;
        this.aiTraceLogger = aiTraceLogger;
        this.outputValidationDomainService = outputValidationDomainService;
        this.aiExecutionAssembler = aiExecutionAssembler;
    }

    public AiExecuteDTO execute(AiExecuteRequest request) {
        AiScenarioBO scenario = new AiScenarioBO(
                request.scenarioCode() == null ? "market_daily_review" : request.scenarioCode(),
                "Market Daily Review",
                "mock-model",
                null,
                6,
                true,
                true,
                true
        );
        AiModelBO model = modelRouteAppService.route(scenario, request.preferredModelCode());
        String prompt = promptRenderAppService.render("Summarize market information with JSON output.", request.userInput(),
                request.variables());
        String content = agentExecutor.execute(scenario, model, prompt, aiToolRegistry.enabledToolCodes());
        boolean valid = outputValidationDomainService.validateJsonRequired(scenario.jsonOutputRequired(), content);
        AiExecutionStatus status = valid ? AiExecutionStatus.SUCCEEDED : AiExecutionStatus.FAILED;
        AiExecutionBO execution = new AiExecutionBO(
                "AI-" + UUID.randomUUID().toString().replace("-", ""),
                scenario,
                model.modelCode(),
                status,
                content,
                aiToolRegistry.enabledToolCodes()
        );
        aiTraceLogger.record(execution.executionNo(), status.name());
        return aiExecutionAssembler.toTarget(execution);
    }
}
