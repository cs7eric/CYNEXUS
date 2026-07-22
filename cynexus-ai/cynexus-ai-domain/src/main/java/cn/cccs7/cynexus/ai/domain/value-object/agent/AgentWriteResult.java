package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentWriteResult {
    boolean success;
    String resultJson;
    String errorMessage;
}
