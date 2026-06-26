package cn.cccs7.cynexus.ai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScenarioBO {

    private String scenarioCode;
    private String scenarioName;
    private String defaultModelCode;
    private String fallbackModelCode;
    private int maxToolCalls;
    private boolean toolCallRequired;
    private boolean jsonOutputRequired;
    private boolean enabled;
}
