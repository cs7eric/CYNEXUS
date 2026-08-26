package cn.cccs7.cynexus.ai.domain.valueobject.scenario;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class PreparedScenarioVisionExecution {
    AiScenarioBO scenario;
    AiModelBO model;
    List<AiModelBO> fallbackModels;
    String prompt;
    String capability;

    public List<AiModelBO> candidateModels() {
        List<AiModelBO> result = new java.util.ArrayList<>();
        if (model != null) result.add(model);
        if (fallbackModels != null) result.addAll(fallbackModels);
        return List.copyOf(result);
    }

    public PreparedScenarioVisionExecution withModel(AiModelBO selectedModel) {
        return PreparedScenarioVisionExecution.builder().scenario(scenario).model(selectedModel)
                .fallbackModels(List.of()).prompt(prompt).capability(capability).build();
    }
}
