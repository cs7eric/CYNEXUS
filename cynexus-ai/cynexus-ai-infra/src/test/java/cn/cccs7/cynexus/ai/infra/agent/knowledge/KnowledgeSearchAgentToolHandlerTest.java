package cn.cccs7.cynexus.ai.infra.agent.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KnowledgeSearchAgentToolHandlerTest {

    @Test
    void searchesPublishedDocumentsWithBoundedResultsAndTypedSchema() throws Exception {
        KnowledgeBaseFeignClient client = mock(KnowledgeBaseFeignClient.class);
        when(client.listDocuments("cash flow", "PUBLISHED", null, null)).thenReturn(Result.success(List.of(
                KnowledgeDocumentDTO.builder().id(9L).title("Cash flow policy").description("Approved policy").status("PUBLISHED").build())));
        KnowledgeSearchAgentToolHandler handler = new KnowledgeSearchAgentToolHandler(client, new ObjectMapper());

        AgentToolResult result = handler.execute(AgentToolCall.builder().toolCode("knowledge.search")
                .arguments(Map.of("query", "cash flow", "limit", 1)).build(), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(new ObjectMapper().readTree(result.getOutputJson())).hasSize(1);
        assertThat(handler.definition().getOutputSchemaJson()).contains("updatedAt").contains("required");
        verify(client).listDocuments("cash flow", "PUBLISHED", null, null);
    }

    @Test
    void refusesBlankSearchInsteadOfCallingKnowledgeBase() {
        KnowledgeBaseFeignClient client = mock(KnowledgeBaseFeignClient.class);
        KnowledgeSearchAgentToolHandler handler = new KnowledgeSearchAgentToolHandler(client, new ObjectMapper());

        AgentToolResult result = handler.execute(AgentToolCall.builder().toolCode("knowledge.search")
                .arguments(Map.of("query", " ")).build(), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("ARGUMENT_INVALID");
        org.mockito.Mockito.verifyNoInteractions(client);
    }
}
