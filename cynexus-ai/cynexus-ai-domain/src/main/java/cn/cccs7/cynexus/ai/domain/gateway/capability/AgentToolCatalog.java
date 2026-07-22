package cn.cccs7.cynexus.ai.domain.gateway.capability;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import java.util.List;
import java.util.Optional;

public interface AgentToolCatalog {

    List<AgentToolDefinition> enabledTools();

    Optional<AgentToolDefinition> find(String toolCode);
}
