package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockDailyFeatureAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_daily_feature")
            .name("Stock daily feature")
            .module("market")
            .description("Read a stock's daily price, valuation and technical feature snapshot.")
            .inputSchemaJson(MarketAgentToolSchemas.STOCK_DAILY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/stock-overview")
            .build();

    public MarketStockDailyFeatureAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String symbol = requiredString(call, "symbol");
        String tradeDate = dateArgument(call, context);
        Object value = marketDataGateway().getStockDailyFeature(symbol, tradeDate.isBlank() ? null : tradeDate);
        return value == null ? empty(DEFINITION, "No daily stock feature was available for " + symbol + ".")
                : success(DEFINITION, value, "Read the daily feature snapshot for " + symbol + ".");
    }
}
