package cn.cccs7.cynexus.ai.domain.agent;

public interface AgentToolHandler {

    AgentToolDefinition definition();

    AgentToolResult execute(AgentToolCall call, AgentExecutionContext context);
}
