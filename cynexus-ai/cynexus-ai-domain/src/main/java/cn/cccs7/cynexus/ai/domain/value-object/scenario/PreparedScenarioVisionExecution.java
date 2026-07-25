package cn.cccs7.cynexus.ai.domain.valueobject.scenario;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreparedScenarioVisionExecution {
    AiScenarioBO scenario;
    AiModelBO model;
    String prompt;
    String capability;
}