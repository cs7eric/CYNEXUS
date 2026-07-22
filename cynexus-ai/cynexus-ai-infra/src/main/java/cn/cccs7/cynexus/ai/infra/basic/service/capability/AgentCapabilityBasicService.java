package cn.cccs7.cynexus.ai.infra.basic.service.capability;

import cn.cccs7.cynexus.ai.infra.basic.entity.capability.AgentCapabilityPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.capability.AgentCapabilityAuditPO;
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
