package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AgentModelTurnRequest {

    String systemPrompt;
    String runtimeContext;
    List<AgentModelMessage> messages;
    List<AgentModelMessage> continuationMessages;
    List<AgentToolDefinition> tools;
    PromptCacheContext promptCacheContext;
    String previousResponseId;

    @Builder.Default
    boolean toolsAllowed = true;
}
