package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.agent.AgentCapabilityAuditEvent;
import java.util.List;

public interface AgentCapabilityRepository {
    void synchronize(List<AgentCapabilityDescriptor> descriptors);

    List<AgentCapabilityDescriptor> listAll();

    List<AgentCapabilityDescriptor> listRuntimeCapabilities();

    boolean updateGovernance(String capabilityCode, boolean enabled, String availability, int priority);

    default void recordAudit(AgentCapabilityAuditEvent event) {
    }
}
