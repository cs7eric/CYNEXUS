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
            case "replay_timeline" -> query(DEFINITION, () -> client().getMarketIndicatorReplayTimeline(
                    date(call, "tradeDate"), optionalString(call, "from", "09:30"), optionalString(call, "to", "15:00"),
                    optionalString(call, "eventType", "all"), optionalString(call, "industryCode", ""),
                    integer(call, "limit", 3000, 10, 5000)),
                    "Read bounded 1m replay evidence, market events, and industry minute snapshots.");
            case "radar_state" -> query(DEFINITION, () -> client().getMarketIndicatorRadarState(
                    date(call, "tradeDate"), optionalString(call, "asOf", "")),
                    "Read the bounded market radar state and breadth context.");
            case "radar_events" -> query(DEFINITION, () -> client().getMarketIndicatorRadarEvents(
                    date(call, "tradeDate"), optionalString(call, "asOf", ""), optionalString(call, "eventType", "all"),
                    integer(call, "limit", 50, 1, 100)),
                    "Read bounded market anomaly events for the requested session.");
            case "limit_ladder" -> query(DEFINITION, () -> client().getMarketIndicatorLimitLadder(
                    date(call, "tradeDate"), optionalString(call, "board", "all"), integer(call, "limit", 100, 1, 100)),
                    "Read the bounded limit-up and limit-down ladder used as event anchors.");
            case "replay_summary" -> query(DEFINITION, () -> client().getMarketIndicatorReplaySummary(
                    date(call, "tradeDate"), integer(call, "limit", 50, 1, 100)),
                    "Read replay coverage, episode, and outcome summary for a trading session.");
            case "replay_episode" -> query(DEFINITION, () -> client().getMarketIndicatorReplayEpisode(
                    requiredString(call, "episodeId"), date(call, "tradeDate")),
                    "Read one bounded sector episode with its timeline, outcomes, and evidence refs.");
            case "trade_review_state" -> query(DEFINITION, () -> client().getTradeReviewMarketState(
                    requiredString(call, "tradeDate"), optionalString(call, "asOf", ""),
                    optionalString(call, "mode", "daily"), optionalString(call, "membership", "union"),
                    optionalString(call, "industryCode", ""), integer(call, "windowDays", 60, 5, 120)),
                    "Read an already materialized Trade Review state; this never calls an external market provider.");
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
