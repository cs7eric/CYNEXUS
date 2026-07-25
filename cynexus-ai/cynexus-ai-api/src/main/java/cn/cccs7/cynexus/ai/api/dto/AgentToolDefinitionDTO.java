package cn.cccs7.cynexus.ai.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentToolDefinitionDTO {
    private String code;
    private String name;
    private String module;
    private String description;
    private String guidanceJson;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private boolean readOnly;
    private boolean requiresConfirmation;
    private String freshness;
    private String source;
    private String sourceUrl;
    private String version;
    private String tenantScope;
    private String availability;
    private int priority;
    private String promptKey;
    private String runtimeEntry;
    private String routeGroup;
}
