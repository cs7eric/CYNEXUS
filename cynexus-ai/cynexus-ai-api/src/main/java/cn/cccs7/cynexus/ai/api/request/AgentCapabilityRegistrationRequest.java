package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

/** Typed admin contract for registering an already allow-listed runtime handler. */
@Data
public class AgentCapabilityRegistrationRequest {
    private String capabilityCode;
    private String version;
    private String name;
    private String module;
    private String description;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Boolean readOnly;
    private Boolean requiresConfirmation;
    private String tenantScope;
    private String source;
    private String sourceUrl;
    private String freshness;
    private String availability;
    private String governanceAvailability;
    private Boolean enabled;
    private Integer priority;
    private String promptKey;
    private String runtimeEntry;
    private String routeGroup;
}
