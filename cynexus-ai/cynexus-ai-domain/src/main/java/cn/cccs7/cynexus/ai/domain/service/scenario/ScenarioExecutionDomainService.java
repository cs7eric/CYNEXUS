package cn.cccs7.cynexus.ai.domain.service.scenario;

import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioTextExecution;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioVisionExecution;
import java.util.List;
import java.util.Map;

/**
 * Domain use case for scenario-based AI execution preparation and finalization.
 * Application may only call this service and then invoke the completion gateway/executor.
 */
public interface ScenarioExecutionDomainService {

    PreparedScenarioTextExecution prepareTextExecution(String scenarioCode,
                                                       String preferredModelCode,
                                                       String userInput,
                                                       Map<String, Object> variables,
                                                       List<String> enabledToolCodes);

    PreparedScenarioVisionExecution prepareVisionExecution(String scenarioCode,
                                                           String preferredModelCode,
                                                           String userInput);

    void validateVisionInput(String scenarioCode, List<AiImageInputBO> images);

    AiExecutionBO finalizeExecution(PreparedScenarioTextExecution prepared, String content);

    AiExecutionBO finalizeVisionExecution(PreparedScenarioVisionExecution prepared, String content);

    AiModelBO routeInformationModel(String preferredModelCode,
                                    List<String> bindingCapabilities,
                                    boolean workspaceChat);

    AiModelBO routeVisionModel(String capability, String preferredModelCode);

    String resolveVisionPrompt(String capability, String candidateContext);

    /**
     * Builds the single bounded repair prompt used when a vision provider
     * returns malformed or schema-incompatible structured output.
     */
    default String resolveVisionRepairPrompt(String capability, String originalPrompt,
                                             String outputSchema, String rawOutput) {
        return originalPrompt;
    }
}
