package cn.cccs7.cynexus.ai.domain.agent;

public interface AgentWriteExecutorPort {
    AgentWriteResult execute(AgentWriteCommandBO command);
}
