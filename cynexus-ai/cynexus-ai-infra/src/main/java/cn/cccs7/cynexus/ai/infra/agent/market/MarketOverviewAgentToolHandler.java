package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketOverviewAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.overview")
            .name("Market overview")
            .module("market")
            .description("Read market breadth, turnover, index and fund-flow overview for a trading date.")
            .inputSchemaJson(MarketAgentToolSchemas.TRADE_DATE)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/stock-overview")
            .build();

    public MarketOverviewAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String tradeDate = dateArgument(call, context);
        Object value = marketDataGateway().getMarketOverview(tradeDate.isBlank() ? null : tradeDate);
        return value == null ? empty(DEFINITION, "No market overview was available for the requested date.")
                : success(DEFINITION, value, "Read the market overview snapshot.");
    }
}
