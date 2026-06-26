package cn.cccs7.cynexus.ai.domain.entity;

public record AiScenarioBO(
        String scenarioCode,
        String scenarioName,
        String defaultModelCode,
        String fallbackModelCode,
        int maxToolCalls,
        boolean toolCallRequired,
        boolean jsonOutputRequired,
        boolean enabled
) {
}
