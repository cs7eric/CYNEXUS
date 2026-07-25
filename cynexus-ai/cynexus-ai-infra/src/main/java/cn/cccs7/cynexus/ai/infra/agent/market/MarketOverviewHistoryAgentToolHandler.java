package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.infra.agent.AgentToolGuidanceCatalog;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class MarketOverviewHistoryAgentToolHandler extends AbstractMarketAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.overview_history")
            .name("Market overview history")
            .module("market")
            .description("Read market overview snapshots across a date range. start/end are optional and default to the last 7 Asia/Shanghai sessions.")
            .guidanceJson(AgentToolGuidanceCatalog.MARKET_OVERVIEW_HISTORY)
            .inputSchemaJson(MarketAgentToolSchemas.HISTORY)
            .outputSchemaJson("{\"type\":\"object\"}")
            .readOnly(true)
            .requiresConfirmation(false)
            .freshness("daily snapshot")
            .source(SOURCE)
            .sourceUrl("/api/market/sentiment/market-overview/history")
            .build();

    public MarketOverviewHistoryAgentToolHandler(MarketDataGateway marketDataGateway, ObjectMapper objectMapper) {
        super(marketDataGateway, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        LocalDate[] range = resolveHistoryRange(call, context);
        Object value = marketDataGateway().getMarketOverviewHistory(range[0].toString(), range[1].toString());
        return value == null
                ? empty(DEFINITION, "No market overview history was available for " + range[0] + ".." + range[1] + ".")
                : success(DEFINITION, value, "Read market overview history for " + range[0] + ".." + range[1] + ".");
    }
}
