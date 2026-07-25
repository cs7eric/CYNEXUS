package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.infra.agent.AgentToolGuidanceCatalog;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockDailyFeatureAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_daily_feature")
            .name("Stock daily feature")
            .module("market")
            .description("Read a stock's daily price, valuation and technical feature snapshot. symbol is required; tradeDate is optional.")
            .guidanceJson(AgentToolGuidanceCatalog.MARKET_STOCK_DAILY)
            .inputSchemaJson(MarketAgentToolSchemas.STOCK_DAILY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/api/market/stocks/{symbol}/daily-feature")
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
        String preferred = resolvedTradeDate(call, context);
        return loadByTradeDate(
                DEFINITION,
                preferred,
                tradeDate -> marketDataGateway().getStockDailyFeature(symbol, tradeDate),
                tradeDate -> "Read the daily feature snapshot for " + symbol + " on " + tradeDate + ".",
                "No daily stock feature was available for " + symbol + " near " + preferred + ".");
    }
}
