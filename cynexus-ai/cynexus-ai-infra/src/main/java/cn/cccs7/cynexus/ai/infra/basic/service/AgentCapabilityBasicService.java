package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentCapabilityPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentCapabilityAuditPO;
import java.util.List;

public interface AgentCapabilityBasicService {
    AgentCapabilityPO find(String code, String version);
    AgentCapabilityPO save(AgentCapabilityPO capability);
    boolean update(AgentCapabilityPO capability);
    List<AgentCapabilityPO> listAll();
    List<AgentCapabilityPO> listRuntime();
    boolean updateGovernance(String code, boolean enabled, String availability, int priority);
    void markUnseenUnavailable(java.time.OffsetDateTime seenAt);
    void markOlderVersionsUnavailable(String code, String version);
}
