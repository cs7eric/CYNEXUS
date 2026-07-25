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
    String prompt;
    List<String> toolCodes;
}