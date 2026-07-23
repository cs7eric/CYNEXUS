package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentWritePreview {
    AgentWriteCommandBO command;
    String confirmationToken;
}
