package cn.cccs7.cynexus.ai.domain.gateway.capability;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;

/** Runtime-side port for atomically exposing an approved capability descriptor. */
public interface AgentCapabilityRuntimeCatalog {
    void register(AgentCapabilityDescriptor descriptor);
}
