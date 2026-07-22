package cn.cccs7.cynexus.ai.domain.agent;

public interface AgentToolExecutor {

    AgentToolResult execute(AgentToolCall call, AgentExecutionContext context);
}
