package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketDataFreshnessAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.data_freshness").name("Market data freshness").module("market")
            .description("Read the current freshness status of market data sources.")
            .guidanceJson(MarketResearchAgentToolGuidance.FRESHNESS)
            .inputSchemaJson(MarketResearchAgentToolSchemas.EMPTY)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("source freshness snapshot").source(SOURCE).sourceUrl("/api/market/data-source/freshness").build();

    public MarketDataFreshnessAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override public AgentToolDefinition definition() { return DEFINITION; }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return query(DEFINITION, client()::getDataSourceFreshness, "Read market data-source freshness.");
    }
}
