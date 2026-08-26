package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Bounded factual single-event replay; no current-user or write state is involved. */
@Component
public class MarketEventReplayAgentToolHandler extends AbstractMarketResearchAgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.event_replay").name("Market event replay").module("market")
            .description("Read one stock's bounded historical minute replay and materialized market/industry coverage around an event.")
            .guidanceJson(MarketResearchAgentToolGuidance.EVENT_REPLAY).inputSchemaJson(MarketResearchAgentToolSchemas.EVENT_REPLAY)
            .outputSchemaJson(MarketResearchAgentToolSchemas.EVENT_REPLAY_OUTPUT).readOnly(true).requiresConfirmation(false)
            .freshness("historical materialized replay and stock minute bars").source(SOURCE).sourceUrl("/api/market/screener/event-replay")
            .version("1.0.0").priority(68).routeGroup("Agent / market").build();
    public MarketEventReplayAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) { super(client, objectMapper); }
    @Override public AgentToolDefinition definition() { return DEFINITION; }
    @Override public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String code = requiredString(call, "code");
        if (!code.matches("[0-9]{6}")) throw new IllegalArgumentException("code must be a six-digit A-share symbol");
        String anchorAt = optionalString(call, "anchorAt", null);
        return query(DEFINITION, () -> client().getMarketEventReplay(code, requiredDate(call, "tradeDate"), anchorAt,
                integer(call, "minuteLimit", 180, 30, 300)), "Read a bounded factual market-event replay.");
    }
    private String requiredDate(AgentToolCall call, String key) { String value = date(call, key); if (value == null) throw new IllegalArgumentException(key + " is required"); return value; }
}
