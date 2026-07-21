package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockIntradayFeatureAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_intraday_feature")
            .name("Stock intraday feature")
            .module("market")
            .description("Read a stock's latest intraday price, volume and market activity features.")
            .inputSchemaJson(MarketAgentToolSchemas.STOCK_INTRADAY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("intraday snapshot")
            .source(SOURCE)
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
        String tradeDate = dateArgument(call, context);
        Object value = marketDataGateway().getStockIntradayFeature(symbol, tradeDate.isBlank() ? null : tradeDate);
        return value == null ? empty(DEFINITION, "No intraday stock feature was available for " + symbol + ".")
                : success(DEFINITION, value, "Read the intraday feature snapshot for " + symbol + ".");
    }
}
