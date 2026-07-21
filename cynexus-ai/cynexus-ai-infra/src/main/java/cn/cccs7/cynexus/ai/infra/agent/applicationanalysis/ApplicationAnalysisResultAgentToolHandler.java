package cn.cccs7.cynexus.ai.infra.agent.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.port.ApplicationAnalysisGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisResultAgentToolHandler implements AgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("application_analysis.result").name("Application analysis result").module("application-analysis")
            .description("Read the persisted application-analysis result for a configured target.")
            .inputSchemaJson("{\"type\":\"object\",\"required\":[\"targetId\"],\"properties\":{\"targetId\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":128}},\"additionalProperties\":false}")
            .outputSchemaJson("{\"type\":\"object\"}").readOnly(true).requiresConfirmation(false)
            .freshness("persisted result").source("Application Analysis").build();
    private final ApplicationAnalysisGateway gateway;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisResultAgentToolHandler(ApplicationAnalysisGateway gateway, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String targetId = String.valueOf((call.getArguments() == null ? Map.of() : call.getArguments()).getOrDefault("targetId", ""));
        Map<String, Object> result = gateway.getResult(targetId);
        if (result == null) return AgentToolResult.failure(DEFINITION.getCode(), "No application analysis result was available.", DEFINITION.getSource(), "DATA_UNAVAILABLE");
        try {
            return AgentToolResult.success(DEFINITION.getCode(), objectMapper.writeValueAsString(result),
                    "Read the persisted application analysis result.", null, DEFINITION.getSource(), List.of());
        } catch (Exception exception) {
            return AgentToolResult.failure(DEFINITION.getCode(), "Application analysis could not be serialized.", DEFINITION.getSource(), "OUTPUT_SERIALIZATION_FAILED");
        }
    }
}
