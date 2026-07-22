package cn.cccs7.cynexus.ai.domain.entity.capability;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/** Governed, typed metadata for a capability that may be exposed to the Agent. */
@Value
@Builder(toBuilder = true)
public class AgentCapabilityDescriptor {
    Long id;
    String capabilityCode;
    String version;
    String name;
    String module;
    String description;
    String inputSchemaJson;
    String outputSchemaJson;
    boolean readOnly;
    boolean requiresConfirmation;
    String tenantScope;
    String source;
    String sourceUrl;
    String freshness;
    String availability;
    String governanceAvailability;
    boolean enabled;
    int priority;
    String promptKey;
    String runtimeEntry;
    String routeGroup;
    String registrationMode;
    OffsetDateTime lastSeenAt;
    OffsetDateTime staleAt;
}
