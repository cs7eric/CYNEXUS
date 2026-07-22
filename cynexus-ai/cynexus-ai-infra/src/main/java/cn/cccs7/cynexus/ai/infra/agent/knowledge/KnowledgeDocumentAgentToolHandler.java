package cn.cccs7.cynexus.ai.infra.agent.knowledge;

import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentAgentToolHandler implements AgentToolHandler {
    private static final String CODE = "knowledge.document";
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder().code(CODE)
            .name("Knowledge document").module("knowledge-base")
            .description("Read one approved Cynexus knowledge-base document by id for grounded analysis.")
            .inputSchemaJson("{\"type\":\"object\",\"required\":[\"documentId\"],\"properties\":{\"documentId\":{\"type\":\"integer\",\"minimum\":1}}}")
            .outputSchemaJson("{\"type\":\"object\",\"required\":[\"id\",\"title\",\"status\",\"content\"],\"properties\":{\"id\":{\"type\":\"integer\"},\"title\":{\"type\":\"string\"},\"description\":{\"type\":[\"string\",\"null\"]},\"content\":{\"type\":\"string\"},\"contentFormat\":{\"type\":[\"string\",\"null\"]},\"status\":{\"type\":\"string\"},\"updatedAt\":{\"type\":[\"string\",\"null\"],\"format\":\"date-time\"}}}").readOnly(true).requiresConfirmation(false)
            .freshness("stored document").source("Knowledge Base API").sourceUrl("/knowledge-base/view").build();

    private final KnowledgeBaseFeignClient client;
    private final ObjectMapper objectMapper;

    public KnowledgeDocumentAgentToolHandler(KnowledgeBaseFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Object rawId = call.getArguments() == null ? null : call.getArguments().get("documentId");
        if (rawId == null) return AgentToolResult.failure(CODE, "documentId is required.", DEFINITION.getSource(), "ARGUMENT_REQUIRED");
        long documentId;
        try { documentId = Long.parseLong(String.valueOf(rawId)); }
        catch (NumberFormatException exception) { return AgentToolResult.failure(CODE, "documentId must be an integer.", DEFINITION.getSource(), "ARGUMENT_INVALID"); }
        Result<KnowledgeDocumentDTO> result = client.getDocument(documentId);
        if (result == null || result.isFailed() || result.getData() == null) {
            return AgentToolResult.failure(CODE, "The knowledge document was not found or is unavailable.", DEFINITION.getSource(), "NO_DATA");
        }
        return AgentToolResult.success(CODE, json(result.getData()), "Read the knowledge-base document.",
                result.getData().getUpdatedAt() == null ? null : result.getData().getUpdatedAt().toLocalDate().toString(),
                DEFINITION.getSource(), List.of());
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Failed to serialize knowledge document", exception); }
    }
}
