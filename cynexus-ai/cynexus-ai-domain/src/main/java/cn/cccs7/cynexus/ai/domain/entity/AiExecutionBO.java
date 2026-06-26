package cn.cccs7.cynexus.ai.domain.entity;

import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import java.util.List;

public record AiExecutionBO(
        String executionNo,
        AiScenarioBO scenario,
        String modelCode,
        AiExecutionStatus status,
        String content,
        List<String> toolCalls
) {
}
