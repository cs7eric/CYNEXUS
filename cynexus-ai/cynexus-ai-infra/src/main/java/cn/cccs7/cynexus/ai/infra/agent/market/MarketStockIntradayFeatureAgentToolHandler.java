package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockIntradayFeatureAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_intraday_feature")
            .name("Stock intraday feature")
            .module("market")
            .description("Read a stock's latest intraday price, volume and market activity features. symbol is required; tradeDate is optional.")
            .inputSchemaJson(MarketAgentToolSchemas.STOCK_INTRADAY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("intraday snapshot")
            .source(SOURCE)
            .sourceUrl("/api/market/stocks/{symbol}/intraday-feature")
            .build();

    public MarketStockIntradayFeatureAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
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
                tradeDate -> marketDataGateway().getStockIntradayFeature(symbol, tradeDate),
                tradeDate -> "Read the intraday feature snapshot for " + symbol + " on " + tradeDate + ".",
                "No intraday stock feature was available for " + symbol + " near " + preferred + ".");
    }
}
