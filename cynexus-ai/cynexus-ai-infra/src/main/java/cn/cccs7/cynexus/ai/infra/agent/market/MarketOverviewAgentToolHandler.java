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
public class MarketOverviewAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.overview")
            .name("Market overview")
            .module("market")
            .description("Read market breadth, turnover, index and fund-flow overview for a trading date. tradeDate is optional and defaults to the latest available Asia/Shanghai session.")
            .guidanceJson(AgentToolGuidanceCatalog.MARKET_OVERVIEW)
            .inputSchemaJson(MarketAgentToolSchemas.TRADE_DATE)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/api/market/overview")
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
        String preferred = resolvedTradeDate(call, context);
        return loadByTradeDate(
                DEFINITION,
                preferred,
                tradeDate -> marketDataGateway().getMarketOverview(tradeDate),
                tradeDate -> "Read the market overview snapshot for " + tradeDate + ".",
                "No market overview was available near " + preferred + ".");
    }
}
