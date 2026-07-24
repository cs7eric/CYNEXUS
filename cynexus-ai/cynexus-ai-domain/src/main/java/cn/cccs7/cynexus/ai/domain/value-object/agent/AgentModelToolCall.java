package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentModelToolCall {

    /** Provider function-call item id (Responses: fc_...). Distinct from callId. */
    String itemId;
    /** Provider call id used by function_call_output.call_id. */
    String callId;
    String toolCode;
    Map<String, Object> arguments;
}
