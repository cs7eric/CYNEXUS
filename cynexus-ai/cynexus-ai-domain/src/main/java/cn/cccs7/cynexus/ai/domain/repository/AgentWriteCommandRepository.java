package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentWriteCommandBO;
import java.util.Optional;

public interface AgentWriteCommandRepository {
    AgentWriteCommandBO save(AgentWriteCommandBO command);
    Optional<AgentWriteCommandBO> findPending(String ownerKey, String commandNo);
    Optional<AgentWriteCommandBO> markConfirmed(String ownerKey, String commandNo, String tokenHash);
    void markExecuted(AgentWriteCommandBO command);
    void markRejected(AgentWriteCommandBO command);
}
