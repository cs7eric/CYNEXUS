package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentToolCall {

    String callId;
    String toolCode;
    Map<String, Object> arguments;
}
