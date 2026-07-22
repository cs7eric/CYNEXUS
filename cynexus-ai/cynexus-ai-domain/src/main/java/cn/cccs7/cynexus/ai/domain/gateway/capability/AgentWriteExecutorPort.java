package cn.cccs7.cynexus.ai.domain.gateway.capability;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWriteResult;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWriteResult;

public interface AgentWriteExecutorPort {
    AgentWriteResult execute(AgentWriteCommandBO command);
}
