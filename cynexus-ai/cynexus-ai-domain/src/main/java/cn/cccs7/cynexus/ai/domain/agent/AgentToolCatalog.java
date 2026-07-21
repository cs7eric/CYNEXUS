package cn.cccs7.cynexus.ai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentToolCatalog {

    List<AgentToolDefinition> enabledTools();

    Optional<AgentToolDefinition> find(String toolCode);
}
