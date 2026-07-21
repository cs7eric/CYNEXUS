package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockChartKlinesAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_chart_klines")
            .name("Stock chart K-lines")
            .module("market")
            .description("Read historical K-line data for a stock, index, sector or ETF.")
            .inputSchemaJson(MarketAgentToolSchemas.KLINES)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("historical chart data")
            .source(SOURCE)
            .build();

    public MarketStockChartKlinesAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String symbol = requiredString(call, "symbol");
        String targetType = optionalString(call, "targetType", "stock");
        String period = optionalString(call, "period", "daily");
        String adjust = optionalString(call, "adjust", "none");
        Object value = marketDataGateway().getStockChartKlines(targetType, symbol, period, adjust);
        return value == null ? empty(DEFINITION, "No K-line data was available for " + symbol + ".")
                : success(DEFINITION, value, "Read historical K-line data for " + symbol + ".");
    }
}
