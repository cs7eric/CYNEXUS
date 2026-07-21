package cn.cccs7.cynexus.ai.domain.agent;

import lombok.Builder;
import lombok.Value;

/** Model-visible metadata. Schemas are JSON text so the domain does not depend on Jackson. */
@Value
@Builder
public class AgentToolDefinition {

    String code;
    String name;
    String module;
    String description;
    String inputSchemaJson;
    String outputSchemaJson;
    boolean readOnly;
    boolean requiresConfirmation;
    String freshness;
    String source;
}
