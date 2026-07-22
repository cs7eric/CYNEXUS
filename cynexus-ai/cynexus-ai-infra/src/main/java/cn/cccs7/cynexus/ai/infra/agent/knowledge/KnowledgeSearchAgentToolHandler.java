package cn.cccs7.cynexus.ai.infra.agent.knowledge;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.handler.toolcall.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSearchAgentToolHandler implements AgentToolHandler {
    private static final String CODE = "knowledge.search";
    private static final int MAX_RESULTS = 10;
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder().code(CODE)
            .name("Knowledge search").module("knowledge-base")
            .description("Search approved Cynexus knowledge-base documents by a bounded keyword query.")
            .inputSchemaJson("{\"type\":\"object\",\"required\":[\"query\"],\"properties\":{\"query\":{\"type\":\"string\",\"minLength\":2,\"maxLength\":240},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":10}},\"additionalProperties\":false}")
            .outputSchemaJson("{\"type\":\"array\",\"items\":{\"type\":\"object\",\"required\":[\"id\",\"title\",\"status\"],\"properties\":{\"id\":{\"type\":\"integer\"},\"title\":{\"type\":\"string\"},\"description\":{\"type\":\"string\"},\"status\":{\"type\":\"string\"},\"updatedAt\":{\"type\":[\"string\",\"null\"]}}}}")
            .readOnly(true).requiresConfirmation(false).freshness("published knowledge documents")
            .source("Knowledge Base API").sourceUrl("/knowledge-base").build();

    private final KnowledgeBaseFeignClient client;
    private final ObjectMapper objectMapper;

    public KnowledgeSearchAgentToolHandler(KnowledgeBaseFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String query = String.valueOf(call.getArguments() == null ? "" : call.getArguments().getOrDefault("query", "")).trim();
        if (query.length() < 2 || query.length() > 240) {
            return AgentToolResult.failure(CODE, "query must contain between 2 and 240 characters.", DEFINITION.getSource(), "ARGUMENT_INVALID");
        }
        int limit = Math.min(MAX_RESULTS, Math.max(1, integer(call.getArguments() == null ? null : call.getArguments().get("limit"), MAX_RESULTS)));
        Result<List<KnowledgeDocumentDTO>> result = client.listDocuments(query, "published", null, null);
        if (result == null || result.isFailed() || result.getData() == null || result.getData().isEmpty()) {
            return AgentToolResult.failure(CODE, "No published knowledge documents matched the query.", DEFINITION.getSource(), "NO_DATA");
        }
        List<Map<String, Object>> rows = result.getData().stream().limit(limit).map(document -> Map.<String, Object>of(
                "id", document.getId(), "title", value(document.getTitle()), "description", value(document.getDescription()),
                "status", value(document.getStatus()), "updatedAt", document.getUpdatedAt() == null ? "" : document.getUpdatedAt().toString()
        )).toList();
        try {
            return AgentToolResult.success(CODE, objectMapper.writeValueAsString(rows), "Found " + rows.size() + " published knowledge documents.",
                    rows.get(0).get("updatedAt") == null ? null : String.valueOf(rows.get(0).get("updatedAt")), DEFINITION.getSource(), List.of());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize knowledge search results", exception);
        }
    }

    private int integer(Object value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String value(String value) { return value == null ? "" : value; }
}
