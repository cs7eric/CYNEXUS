package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketIndicatorAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.indicator").name("Market indicator research").module("market")
            .description("Read bounded market-indicator snapshots, sector transmission episodes, replay outcomes, conditional trade references, and industry detail.")
            .guidanceJson(MarketResearchAgentToolGuidance.INDICATOR)
            .inputSchemaJson(MarketResearchAgentToolSchemas.INDICATOR)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("market indicator snapshot").source(SOURCE).sourceUrl("/api/market/indicator").build();

    public MarketIndicatorAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        return switch (operation(call)) {
            case "latest" -> query(DEFINITION, () -> client().getMarketIndicatorLatest(
                    date(call, "tradeDate"), optionalString(call, "status", "all"),
                    optionalString(call, "eventType", "all"), integer(call, "limit", 100, 1, 300)),
                    "Read the latest market-indicator snapshot.");
            case "industry_detail" -> query(DEFINITION, () -> client().getMarketIndicatorIndustryDetail(
                    requiredString(call, "industryCode"), date(call, "tradeDate"),
                    nonNegativeInteger(call, "page", 0, 0, 100), integer(call, "pageSize", 50, 10, 100)),
                    "Read market-indicator detail for the requested industry.");
            case "transmission" -> query(DEFINITION, () -> client().getMarketIndicatorTransmission(
                    date(call, "tradeDate"), integer(call, "limit", 100, 1, 300)),
                    "Read sector transmission episodes, conditional references, and replay outcomes.");
            default -> throw new IllegalArgumentException("Unsupported market-indicator operation: " + operation(call));
        };
    }

    private int nonNegativeInteger(AgentToolCall call, String key, int fallback, int min, int max) {
        String raw = optionalString(call, key, String.valueOf(fallback));
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }
}
