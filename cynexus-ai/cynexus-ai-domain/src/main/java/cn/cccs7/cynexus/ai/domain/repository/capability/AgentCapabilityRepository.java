package cn.cccs7.cynexus.ai.domain.repository.capability;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import java.util.List;

public interface AgentCapabilityRepository {
    void synchronize(List<AgentCapabilityDescriptor> descriptors);

    List<AgentCapabilityDescriptor> listAll();

    List<AgentCapabilityDescriptor> listRuntimeCapabilities();

    boolean updateGovernance(String capabilityCode, boolean enabled, String availability, int priority);

}
