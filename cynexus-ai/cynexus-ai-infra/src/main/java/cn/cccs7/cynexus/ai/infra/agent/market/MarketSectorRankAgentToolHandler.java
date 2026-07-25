package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketSectorRankAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.sector_rank")
            .name("Sector ranking")
            .module("market")
            .description("Read sector ranking by change percent / net inflow for a trading date. tradeDate is optional.")
            .inputSchemaJson(MarketAgentToolSchemas.SECTOR_RANK)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/api/market/sectors/rank")
            .build();

    public MarketSectorRankAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String preferred = resolvedTradeDate(call, context);
        int topN = integerArgument(call, "topN", 10);
        return loadByTradeDate(
                DEFINITION,
                preferred,
                tradeDate -> marketDataGateway().getSectorRank(tradeDate, topN),
                tradeDate -> "Read top " + topN + " sector ranks for " + tradeDate + ".",
                "No sector ranking was available near " + preferred + ".");
    }
}
