package cn.cccs7.cynexus.ai.domain.handler.toolcall;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;

public interface AgentToolHandler {

    AgentToolDefinition definition();

    AgentToolResult execute(AgentToolCall call, AgentExecutionContext context);

    default AgentCapabilityDescriptor capabilityDescriptor() {
        AgentToolDefinition definition = definition();
        return AgentCapabilityDescriptor.builder()
                .capabilityCode(definition.getCode())
                .version(definition.getVersion() == null ? "1.0.0" : definition.getVersion())
                .name(definition.getName())
                .module(definition.getModule())
                .description(definition.getDescription())
                .guidanceJson(definition.getGuidanceJson())
                .inputSchemaJson(definition.getInputSchemaJson())
                .outputSchemaJson(definition.getOutputSchemaJson())
                .readOnly(definition.isReadOnly())
                .requiresConfirmation(definition.isRequiresConfirmation())
                .tenantScope(definition.getTenantScope() == null ? "TENANT" : definition.getTenantScope())
                .source(definition.getSource())
                .sourceUrl(definition.getSourceUrl())
                .freshness(definition.getFreshness())
                .availability(definition.getAvailability() == null ? "AVAILABLE" : definition.getAvailability())
                .governanceAvailability("AVAILABLE")
                .enabled(true)
                .priority(definition.getPriority())
                .promptKey(definition.getPromptKey() == null ? "autonomous_agent" : definition.getPromptKey())
                .runtimeEntry(definition.getRuntimeEntry() == null ? getClass().getName() : definition.getRuntimeEntry())
                .routeGroup(definition.getRouteGroup() == null ? "Agent / " + definition.getModule() : definition.getRouteGroup())
                .registrationMode("HANDLER")
                .build();
    }
}
