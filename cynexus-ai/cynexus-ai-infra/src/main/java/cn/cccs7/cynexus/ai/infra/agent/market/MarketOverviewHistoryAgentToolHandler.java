package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketOverviewHistoryAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.overview_history")
            .name("Market overview history")
            .module("market")
            .description("Read market overview snapshots across a requested date range for comparison.")
            .inputSchemaJson(MarketAgentToolSchemas.HISTORY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .build();

    public MarketOverviewHistoryAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Object value = marketDataGateway().getMarketOverviewHistory(stringArgument(call, "start"), stringArgument(call, "end"));
        return value == null ? empty(DEFINITION, "No market overview history was available.")
                : success(DEFINITION, value, "Read market overview history for comparison.");
    }
}
