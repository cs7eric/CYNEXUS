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
            .description("Read leading and lagging sector rankings for a trading date.")
            .inputSchemaJson(MarketAgentToolSchemas.SECTOR_RANK)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/stock-overview/market")
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
        String tradeDate = dateArgument(call, context);
        Object value = marketDataGateway().getSectorRank(tradeDate.isBlank() ? null : tradeDate,
                integerArgument(call, "topN", 10));
        return value == null ? empty(DEFINITION, "No sector ranking was available.")
                : success(DEFINITION, value, "Read sector ranking and direction data.");
    }
}
