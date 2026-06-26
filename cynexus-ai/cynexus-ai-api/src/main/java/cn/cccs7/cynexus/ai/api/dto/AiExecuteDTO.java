package cn.cccs7.cynexus.ai.api.dto;

import java.util.List;

public record AiExecuteDTO(
        String executionNo,
        String scenarioCode,
        String modelCode,
        String status,
        String content,
        List<String> toolCalls
) {
}
