package cn.cccs7.cynexus.ai.infra.agent.finance;

import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolHandler;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceOverviewDTO;
import cn.cccs7.cynexus.core.api.feign.FinanceDataFeignClient;
import cn.cccs7.cynexus.common.core.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinanceOverviewAgentToolHandler implements AgentToolHandler {
    private static final String CODE = "finance.overview";
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder().code(CODE)
            .name("Finance overview").module("finance")
            .description("Read income, expense, net assets, account balances and recent finance summary for a date range.")
            .inputSchemaJson(FinanceAgentToolSchemas.DATE_RANGE).outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true).requiresConfirmation(false).freshness("ledger snapshot").source("Core Finance API")
            .sourceUrl("/finance/overview").build();

    private final FinanceDataFeignClient client;
    private final ObjectMapper objectMapper;

    public FinanceOverviewAgentToolHandler(FinanceDataFeignClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        LocalDate start = date(call, "start");
        LocalDate end = date(call, "end");
        Result<FinanceOverviewDTO> result = client.getOverview(start, end);
        if (result == null || result.isFailed() || result.getData() == null) {
            return AgentToolResult.failure(CODE, "No finance overview was available for the requested range.", DEFINITION.getSource(), "NO_DATA");
        }
        FinanceOverviewDTO value = result.getData();
        return AgentToolResult.success(CODE, json(value), "Read the finance overview.",
                value.getEndDate() == null ? null : value.getEndDate().toString(), DEFINITION.getSource(), List.of());
    }

    private LocalDate date(AgentToolCall call, String key) {
        Object value = call.getArguments() == null ? null : call.getArguments().get(key);
        return value == null || String.valueOf(value).isBlank() ? null : LocalDate.parse(String.valueOf(value));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Failed to serialize finance overview", exception); }
    }
}
