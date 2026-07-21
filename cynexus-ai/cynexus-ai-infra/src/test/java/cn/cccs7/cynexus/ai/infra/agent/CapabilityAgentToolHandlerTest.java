package cn.cccs7.cynexus.ai.infra.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.infra.agent.finance.FinanceOverviewAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.finance.FinanceTransactionsAgentToolHandler;
import cn.cccs7.cynexus.ai.infra.agent.knowledge.KnowledgeDocumentAgentToolHandler;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceOverviewDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import cn.cccs7.cynexus.core.api.feign.FinanceDataFeignClient;
import cn.cccs7.cynexus.knowledgebase.api.feign.KnowledgeBaseFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CapabilityAgentToolHandlerTest {

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void financeOverviewUsesReadOnlyCoreApiAndReturnsDate() {
        FinanceDataFeignClient client = mock(FinanceDataFeignClient.class);
        when(client.getOverview(null, null)).thenReturn(Result.success(FinanceOverviewDTO.builder()
                .endDate(LocalDate.of(2026, 7, 22)).netAssets(new BigDecimal("100.00")).build()));

        var result = new FinanceOverviewAgentToolHandler(client, objectMapper()).execute(
                AgentToolCall.builder().toolCode("finance.overview").arguments(java.util.Map.of()).build(), null);

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getDataDate()).isEqualTo("2026-07-22");
        assertThat(result.getOutputJson()).contains("100.00");
    }

    @Test
    void financeTransactionsBoundsLimitAndKeepsToolReadOnly() {
        FinanceDataFeignClient client = mock(FinanceDataFeignClient.class);
        when(client.getTransactions(null, null, 100)).thenReturn(Result.success(List.of(FinanceTransactionDTO.builder().amount(new BigDecimal("2.50")).build())));
        var handler = new FinanceTransactionsAgentToolHandler(client, objectMapper());

        var result = handler.execute(AgentToolCall.builder().toolCode("finance.transactions")
                .arguments(java.util.Map.of("limit", 999)).build(), null);

        assertThat(handler.definition().isReadOnly()).isTrue();
        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getOutputJson()).contains("2.50");
    }

    @Test
    void knowledgeDocumentRejectsMissingIdBeforeCallingApi() {
        KnowledgeBaseFeignClient client = mock(KnowledgeBaseFeignClient.class);
        var result = new KnowledgeDocumentAgentToolHandler(client, objectMapper()).execute(
                AgentToolCall.builder().toolCode("knowledge.document").arguments(java.util.Map.of()).build(), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getWarnings()).contains("ARGUMENT_REQUIRED");
    }
}
