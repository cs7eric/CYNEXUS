package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.application.assembler.AiExecutionAssembler;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionTracePort;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiToolCatalog;
import cn.cccs7.cynexus.ai.domain.service.scenario.ScenarioExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioTextExecution;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioVisionExecution;
import java.util.List;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.common.core.constant.TraceId;
import org.springframework.stereotype.Service;

/**
 * Application orchestration for scenario execution.
 * Business rules (scenario resolution, routing, prompt, validation) live in domain.
 */
@Service
public class ScenarioExecutionAppService {

    private final AiModelExecutor modelExecutor;
    private final AiToolCatalog aiToolCatalog;
    private final AiExecutionTracePort aiExecutionTracePort;
    private final AiExecutionAssembler aiExecutionAssembler;
    private final ScenarioExecutionDomainService scenarioExecutionDomainService;

    public ScenarioExecutionAppService(AiModelExecutor modelExecutor,
                                       AiToolCatalog aiToolCatalog,
                                       AiExecutionTracePort aiExecutionTracePort,
                                       AiExecutionAssembler aiExecutionAssembler,
                                       ScenarioExecutionDomainService scenarioExecutionDomainService) {
        this.modelExecutor = modelExecutor;
        this.aiToolCatalog = aiToolCatalog;
        this.aiExecutionTracePort = aiExecutionTracePort;
        this.aiExecutionAssembler = aiExecutionAssembler;
        this.scenarioExecutionDomainService = scenarioExecutionDomainService;
    }

    public AiExecuteDTO execute(AiExecuteRequest request) {
        return execute(request, null, null);
    }

    public AiExecuteDTO execute(AiExecuteRequest request, String tenantKey, String principalKey) {
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
                if (prepared.getToolCodes() != null && !prepared.getToolCodes().isEmpty()) {
                    throw new IllegalStateException("Tool-enabled generic execution is unsupported; use the autonomous Agent workflow.");
                }
                String executionNo = prepared.getExecutionNo();
                AiExecutionContext context = new AiExecutionContext(executionNo, prepared.getScenario().getScenarioCode(),
                        prepared.getPromptKey(), prepared.getPromptVersion(), model.getProviderCode(), model.getModelCode(),
                        tenantKey, principalKey, executionNo, TraceId.current());
                String content = modelExecutor.complete(model, prepared.getPrompt(), context).getContent();
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
        return executeVision(request, null, null);
    }

    public AiExecuteDTO executeVision(AiVisionExecuteRequest request, String tenantKey, String principalKey) {
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
                String executionNo = prepared.getExecutionNo();
                AiExecutionContext context = new AiExecutionContext(executionNo, prepared.getCapability(),
                        prepared.getPromptKey(), prepared.getPromptVersion(), model.getProviderCode(), model.getModelCode(),
                        tenantKey, principalKey, executionNo, TraceId.current());
                var completion = modelExecutor.completeVision(model, prepared.getPrompt(), images, context);
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
