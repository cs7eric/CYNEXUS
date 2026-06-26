package cn.cccs7.cynexus.ai.api.dto;

public record AiScenarioDTO(
        String scenarioCode,
        String scenarioName,
        String defaultModelCode,
        String fallbackModelCode,
        boolean toolCallRequired,
        boolean jsonOutputRequired,
        boolean enabled
) {
}
