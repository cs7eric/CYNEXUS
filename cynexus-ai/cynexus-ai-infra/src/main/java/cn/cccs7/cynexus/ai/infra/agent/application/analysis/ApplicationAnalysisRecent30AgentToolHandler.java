package cn.cccs7.cynexus.ai.infra.agent.application.analysis;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.port.ApplicationAnalysisGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisRecent30AgentToolHandler implements AgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("application_analysis.recent30").name("Application analysis recent 30 days").module("application-analysis")
            .description("Read persisted daily snapshots for an application-analysis target.")
            .inputSchemaJson("{\"type\":\"object\",\"required\":[\"targetId\"],\"properties\":{\"targetId\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":128},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":60}},\"additionalProperties\":false}")
            .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).requiresConfirmation(false)
            .freshness("daily snapshots").source("Application Analysis")
            .sourceUrl("/stock-overview/application-analysis").build();
    private final ApplicationAnalysisGateway gateway;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisRecent30AgentToolHandler(ApplicationAnalysisGateway gateway, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Map<String, Object> args = call.getArguments() == null ? Map.of() : call.getArguments();
        String targetId = String.valueOf(args.getOrDefault("targetId", ""));
        int limit;
        try { limit = Math.max(1, Math.min(60, Integer.parseInt(String.valueOf(args.getOrDefault("limit", 30))))); }
        catch (NumberFormatException exception) { return AgentToolResult.failure(DEFINITION.getCode(), "The snapshot limit is invalid.", DEFINITION.getSource(), "ARGUMENT_TYPE:limit"); }
        Map<String, Object> result = gateway.getRecent30(targetId, limit);
        if (result == null) return AgentToolResult.failure(DEFINITION.getCode(), "No application snapshots were available.", DEFINITION.getSource(), "DATA_UNAVAILABLE");
        try {
            return AgentToolResult.success(DEFINITION.getCode(), objectMapper.writeValueAsString(result),
                    "Read the persisted application analysis snapshots.", null, DEFINITION.getSource(), List.of());
        } catch (Exception exception) {
            return AgentToolResult.failure(DEFINITION.getCode(), "Application snapshots could not be serialized.", DEFINITION.getSource(), "OUTPUT_SERIALIZATION_FAILED");
        }
    }
}
