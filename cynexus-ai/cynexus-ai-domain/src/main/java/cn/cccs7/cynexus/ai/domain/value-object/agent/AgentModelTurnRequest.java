package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentModelTurnRequest {

    String systemPrompt;
    List<AgentModelMessage> messages;
    List<AgentToolDefinition> tools;
}
