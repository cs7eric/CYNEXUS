package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.application.assembler.AiExecutionAssembler;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionTracePort;
import cn.cccs7.cynexus.ai.domain.port.AiToolCatalog;
import cn.cccs7.cynexus.ai.domain.service.scenario.ScenarioExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioTextExecution;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioVisionExecution;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application orchestration for scenario execution.
 * Business rules (scenario resolution, routing, prompt, validation) live in domain.
 */
@Service
public class ScenarioExecutionAppService {

    private final AgentExecutor agentExecutor;
    private final AiToolCatalog aiToolCatalog;
    private final AiExecutionTracePort aiExecutionTracePort;
    private final AiExecutionAssembler aiExecutionAssembler;
    private final ScenarioExecutionDomainService scenarioExecutionDomainService;

    public ScenarioExecutionAppService(AgentExecutor agentExecutor,
                                       AiToolCatalog aiToolCatalog,
                                       AiExecutionTracePort aiExecutionTracePort,
                                       AiExecutionAssembler aiExecutionAssembler,
                                       ScenarioExecutionDomainService scenarioExecutionDomainService) {
        this.agentExecutor = agentExecutor;
        this.aiToolCatalog = aiToolCatalog;
        this.aiExecutionTracePort = aiExecutionTracePort;
        this.aiExecutionAssembler = aiExecutionAssembler;
        this.scenarioExecutionDomainService = scenarioExecutionDomainService;
    }

    public AiExecuteDTO execute(AiExecuteRequest request) {
        AiExecuteRequest safeRequest = request == null ? new AiExecuteRequest() : request;
        PreparedScenarioTextExecution prepared = scenarioExecutionDomainService.prepareTextExecution(
                safeRequest.getScenarioCode(),
                safeRequest.getPreferredModelCode(),
                safeRequest.getUserInput(),
                safeRequest.getVariables(),
                aiToolCatalog.enabledToolCodes());
        RuntimeException lastFailure = null;
        for (var model : prepared.candidateModels()) {
            try {
                String content = agentExecutor.execute(prepared.getScenario(), model, prepared.getPrompt(), prepared.getToolCodes());
                AiExecutionBO execution = scenarioExecutionDomainService.finalizeExecution(prepared.withModel(model), content);
                aiExecutionTracePort.record(execution.getExecutionNo(), execution.getStatus());
                if (execution.getStatus() == cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus.SUCCEEDED) {
                    return aiExecutionAssembler.toDTO(execution);
                }
                lastFailure = new IllegalStateException("AI returned invalid output for model " + model.getModelCode());
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new IllegalStateException("AI scenario execution failed for all available models", lastFailure);
    }

    public AiExecuteDTO executeVision(AiVisionExecuteRequest request) {
        PreparedScenarioVisionExecution prepared = scenarioExecutionDomainService.prepareVisionExecution(
                request == null ? null : request.getScenarioCode(),
                request == null ? null : request.getPreferredModelCode(),
                request == null ? null : request.getUserInput());
        List<AiImageInputBO> images = request == null || request.getImages() == null
                ? List.of()
                : request.getImages().stream()
                        .map(item -> AiImageInputBO.builder()
                                .fileName(item.getFileName())
                                .contentType(item.getContentType())
                                .dataUrl(item.getDataUrl())
                                .build())
                        .toList();
        RuntimeException lastFailure = null;
        for (var model : prepared.candidateModels()) {
            try {
                var completion = agentExecutor.completeVisionDetailed(model, prepared.getPrompt(), images, prepared.getCapability());
                AiExecutionBO execution = scenarioExecutionDomainService.finalizeVisionExecution(prepared.withModel(model), completion.getContent());
                aiExecutionTracePort.record(execution.getExecutionNo(), execution.getStatus());
                if (execution.getStatus() == cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus.SUCCEEDED) {
                    return aiExecutionAssembler.toDTO(execution);
                }
                lastFailure = new IllegalStateException("AI returned invalid vision output for model " + model.getModelCode());
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new IllegalStateException("AI vision execution failed for all available models", lastFailure);
    }
}
