package cn.cccs7.cynexus.ai.domain.valueobject.scenario;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreparedScenarioTextExecution {
    AiScenarioBO scenario;
    AiModelBO model;
    List<AiModelBO> fallbackModels;
    String prompt;
    List<String> toolCodes;
    String promptKey;
    String promptVersion;
    String executionNo;
    String outputSchema;

    public List<AiModelBO> candidateModels() {
        List<AiModelBO> result = new java.util.ArrayList<>();
        if (model != null) result.add(model);
        if (fallbackModels != null) result.addAll(fallbackModels);
        return List.copyOf(result);
    }

    public PreparedScenarioTextExecution withModel(AiModelBO selectedModel) {
        return PreparedScenarioTextExecution.builder().scenario(scenario).model(selectedModel)
                .fallbackModels(List.of()).prompt(prompt).toolCodes(toolCodes)
                .promptKey(promptKey).promptVersion(promptVersion).executionNo(executionNo).outputSchema(outputSchema).build();
    }
}
