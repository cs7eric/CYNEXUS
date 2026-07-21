package cn.cccs7.cynexus.ai.domain.agent;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentModelToolCall {

    String callId;
    String toolCode;
    Map<String, Object> arguments;
}
