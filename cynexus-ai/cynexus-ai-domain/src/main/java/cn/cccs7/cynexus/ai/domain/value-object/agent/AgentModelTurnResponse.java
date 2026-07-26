package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AgentModelTurnResponse {

    String type;
    String content;
    List<AgentModelToolCall> toolCalls;
    Integer inputTokens;
    Integer cachedInputTokens;
    Integer cacheWriteTokens;
    Integer outputTokens;
    Integer totalTokens;
    String providerResponseId;
    PromptCacheTelemetry cacheTelemetry;
    String rawResponse;

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
