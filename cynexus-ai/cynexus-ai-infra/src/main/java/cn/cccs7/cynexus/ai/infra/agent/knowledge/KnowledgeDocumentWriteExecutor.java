package cn.cccs7.cynexus.ai.infra.agent.knowledge;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentWriteExecutorPort;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWriteResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentWriteExecutor implements AgentWriteExecutorPort {
    private final KnowledgeBaseFeignClient client;
    private final ObjectMapper objectMapper;

    public KnowledgeDocumentWriteExecutor(KnowledgeBaseFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentWriteResult execute(AgentWriteCommandBO command) {
        try {
            Map<String, Object> args = objectMapper.readValue(command.getArgumentsJson(), new TypeReference<>() { });
            KnowledgeDocumentDTO document = KnowledgeDocumentDTO.builder()
                    .title(text(args.get("title"))).description(text(args.get("description")))
                    .content(text(args.get("content"))).contentFormat(value(args.get("contentFormat"), "markdown"))
                    .status("DRAFT").build();
            Result<Long> result = client.createDocument(document);
            if (result == null || result.isFailed() || result.getData() == null) {
                return AgentWriteResult.builder().success(false).errorMessage("Knowledge document creation was rejected by the Core service.").build();
            }
            return AgentWriteResult.builder().success(true).resultJson(objectMapper.writeValueAsString(Map.of(
                    "documentId", result.getData(), "status", "DRAFT", "source", "Knowledge Base API"))).build();
        } catch (Exception exception) {
            return AgentWriteResult.builder().success(false).errorMessage("Knowledge document creation failed safely.").build();
        }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String value(Object value, String fallback) { String text = text(value); return text.isBlank() ? fallback : text; }
}
