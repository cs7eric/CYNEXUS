package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentWriteCommandPO;
import java.time.OffsetDateTime;

public interface AgentWriteCommandBasicService {
    AgentWriteCommandPO save(AgentWriteCommandPO command);
    AgentWriteCommandPO findPending(String ownerKey, String commandNo);
    AgentWriteCommandPO markConfirmed(String ownerKey, String commandNo, String tokenHash, OffsetDateTime now);
    void markExecuted(AgentWriteCommandPO command);
    void markRejected(AgentWriteCommandPO command);
}
