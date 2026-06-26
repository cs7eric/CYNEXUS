package cn.cccs7.cynexus.ai.application.command;

import java.util.Map;

public record ExecuteScenarioCommand(
        String scenarioCode,
        String userInput,
        Map<String, Object> variables,
        String preferredModelCode
) {
}
