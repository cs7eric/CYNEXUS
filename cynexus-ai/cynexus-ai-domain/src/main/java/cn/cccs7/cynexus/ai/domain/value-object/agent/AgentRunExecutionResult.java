package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentRunExecutionResult {
    AgentRunBO run;
    List<AgentRunStepBO> steps;
}
