package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketStockChartKlinesAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_chart_klines")
            .name("Stock chart K-lines")
            .module("market")
            .description("Read historical K-line bars for a stock/index/sector/ETF. symbol is required.")
            .inputSchemaJson(MarketAgentToolSchemas.KLINES)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("historical chart data")
            .source(SOURCE)
            .sourceUrl("/api/market/stock-chart/klines")
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
        String period = optionalString(call, "period", "day");
        String adjust = optionalString(call, "adjust", "qfq");
        Object value = marketDataGateway().getStockChartKlines(targetType, symbol, period, adjust);
        if (value == null) {
            return empty(DEFINITION, "No K-line data was available for symbol=" + symbol
                    + ", targetType=" + targetType + ", period=" + period + ", adjust=" + adjust + ".");
        }
        return success(DEFINITION, value, "Read K-line chart data for " + symbol + ".");
    }
}
