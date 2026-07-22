package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentCapabilityDTO {
    private Long id;
    private String capabilityCode;
    private String version;
    private String name;
    private String module;
    private String description;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private boolean readOnly;
    private boolean requiresConfirmation;
    private String tenantScope;
    private String source;
    private String sourceUrl;
    private String freshness;
    private String availability;
    private String governanceAvailability;
    private boolean enabled;
    private int priority;
    private String promptKey;
    private String runtimeEntry;
    private String routeGroup;
    private String registrationMode;
    private String lastSeenAt;
    private String staleAt;
}
