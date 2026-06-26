package cn.cccs7.cynexus.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScenarioDTO {

    private String scenarioCode;
    private String scenarioName;
    private String defaultModelCode;
    private String fallbackModelCode;
    private boolean toolCallRequired;
    private boolean jsonOutputRequired;
    private boolean enabled;
}
