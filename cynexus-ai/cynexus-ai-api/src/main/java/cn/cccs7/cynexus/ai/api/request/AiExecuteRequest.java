package cn.cccs7.cynexus.ai.api.request;

import java.util.Map;

public record AiExecuteRequest(
        String scenarioCode,
        String userInput,
        Map<String, Object> variables,
        String preferredModelCode
) {
}
