package cn.cccs7.cynexus.ai.infra.observability;

import org.springframework.stereotype.Component;

@Component
public class AiTokenUsageRecorder {

    public void record(String executionNo, int inputTokens, int outputTokens) {
        // Persist token usage here after ai_execution_log is wired.
    }
}
