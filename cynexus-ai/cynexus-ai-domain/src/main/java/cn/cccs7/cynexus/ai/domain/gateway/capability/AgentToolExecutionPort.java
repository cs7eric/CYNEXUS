package cn.cccs7.cynexus.ai.domain.gateway.capability;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;

/** Cancellation-aware tool call supplied by the application adapter. */
@FunctionalInterface
public interface AgentToolExecutionPort {

    AgentToolResult execute(AgentToolCall call, AgentExecutionContext context);
}
