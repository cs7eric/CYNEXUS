package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketEtfAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.etf").name("ETF market research").module("market")
            .description("Read a bounded ETF market snapshot with trend, breadth, rotation, and flow proxies.")
            .guidanceJson(MarketResearchAgentToolGuidance.ETF)
            .inputSchemaJson(MarketResearchAgentToolSchemas.ETF)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("ETF market snapshot").source(SOURCE).sourceUrl("/api/market/etf").build();

    public MarketEtfAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return query(DEFINITION, () -> client().getMarketEtf(
                        integer(call, "historyDays", 10, 3, 30),
                        integer(call, "rowLimit", 180, 20, 300)),
                "Read the bounded ETF market snapshot.");
    }
}
