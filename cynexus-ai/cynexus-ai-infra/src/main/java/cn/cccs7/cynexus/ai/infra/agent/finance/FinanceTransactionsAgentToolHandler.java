package cn.cccs7.cynexus.ai.infra.agent.finance;

import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceTransactionDTO;
import cn.cccs7.cynexus.core.api.feign.FinanceDataFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FinanceTransactionsAgentToolHandler implements AgentToolHandler {
    private static final String CODE = "finance.transactions";
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder().code(CODE)
            .name("Finance transactions").module("finance")
            .description("Read a bounded list of ledger transactions for a date range; use for spending details and audit context.")
            .inputSchemaJson(FinanceAgentToolSchemas.TRANSACTIONS).outputSchemaJson(FinanceAgentToolSchemas.TRANSACTIONS_OUTPUT)
            .readOnly(true).requiresConfirmation(false).freshness("ledger transactions").source("Core Finance API")
            .sourceUrl("/finance/transactions").build();

    private final FinanceDataFeignClient client;
    private final ObjectMapper objectMapper;

    public FinanceTransactionsAgentToolHandler(FinanceDataFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        LocalDate start = date(call, "start");
        LocalDate end = date(call, "end");
        int limit = number(call, "limit", 50);
        Result<List<FinanceTransactionDTO>> result = client.getTransactions(start, end, limit);
        if (result == null || result.isFailed() || result.getData() == null || result.getData().isEmpty()) {
            return AgentToolResult.failure(CODE, "No finance transactions were available for the requested range.", DEFINITION.getSource(), "NO_DATA");
        }
        return AgentToolResult.success(CODE, json(result.getData()), "Read " + result.getData().size() + " finance transactions.",
                end == null ? null : end.toString(), DEFINITION.getSource(), List.of());
    }

    private LocalDate date(AgentToolCall call, String key) {
        Object value = call.getArguments() == null ? null : call.getArguments().get(key);
        return value == null || String.valueOf(value).isBlank() ? null : LocalDate.parse(String.valueOf(value));
    }

    private int number(AgentToolCall call, String key, int fallback) {
        Object value = call.getArguments() == null ? null : call.getArguments().get(key);
        if (value == null) return fallback;
        return Math.max(1, Math.min(100, Integer.parseInt(String.valueOf(value))));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Failed to serialize finance transactions", exception); }
    }
}
