package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MarketFuturesPositionAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.futures_position").name("Futures position research").module("market")
            .description("Read bounded futures-position aggregates, member rankings, history, and source coverage.")
            .guidanceJson(MarketResearchAgentToolGuidance.FUTURES_POSITION)
            .inputSchemaJson(MarketResearchAgentToolSchemas.FUTURES_POSITION)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EMPTY).readOnly(true).requiresConfirmation(false)
            .freshness("futures position snapshot").source(SOURCE).sourceUrl("/api/market/futures-position").build();

    public MarketFuturesPositionAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String scopeType = optionalString(call, "scopeType", "CONTRACT");
        String exchangeCode = optionalString(call, "exchangeCode", null);
        String instrumentCode = optionalString(call, "instrumentCode", null);
        int top = integer(call, "top", 10, 1, 20);
        String sort = optionalString(call, "sort", "netPosition");
        String sortDir = optionalString(call, "sortDir", "desc");
        return switch (operation(call)) {
            case "latest" -> query(DEFINITION, () -> client().getFuturesPositionLatest(
                    date(call, "tradeDate"), scopeType, exchangeCode, optionalString(call, "varietyCode", null), top,
                    sort, sortDir, optionalInteger(call, "page", 1, 1, 100), optionalInteger(call, "pageSize", 30, 1, 100)),
                    "Read the latest futures-position aggregate.");
            case "instruments" -> query(DEFINITION, () -> client().getFuturesPositionInstruments(
                    date(call, "tradeDate"), scopeType, exchangeCode, optionalString(call, "varietyCode", null), top,
                    sort, sortDir, optionalInteger(call, "page", 1, 1, 100), optionalInteger(call, "pageSize", 30, 1, 100)),
                    "Read futures-position instruments.");
            case "members" -> query(DEFINITION, () -> client().getFuturesPositionMembers(
                    scopeType, requiredString(call, "exchangeCode"), requiredString(call, "instrumentCode"),
                    date(call, "tradeDate"), optionalString(call, "rankType", null), top),
                    "Read futures-position member rankings.");
            case "history" -> query(DEFINITION, () -> client().getFuturesPositionHistory(
                    scopeType, requiredString(call, "exchangeCode"), requiredString(call, "instrumentCode"),
                    date(call, "tradeDate"), top, integer(call, "limit", 120, 1, 120)),
                    "Read futures-position history.");
            case "coverage" -> query(DEFINITION, () -> client().getFuturesPositionCoverage(date(call, "tradeDate")),
                    "Read futures-position source coverage.");
            default -> throw new IllegalArgumentException("Unsupported futures-position operation: " + operation(call));
        };
    }

    private Integer optionalInteger(AgentToolCall call, String key, int fallback, int min, int max) {
        return integer(call, key, fallback, min, max);
    }
}
