package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;
import java.util.List;

/** Provider-neutral transcript item used by an Agent model turn. */
@Value
@Builder
public class AgentModelMessage {

    String role;
    String content;
    String toolCallId;
    String toolName;
    List<AgentModelToolCall> toolCalls;
}
