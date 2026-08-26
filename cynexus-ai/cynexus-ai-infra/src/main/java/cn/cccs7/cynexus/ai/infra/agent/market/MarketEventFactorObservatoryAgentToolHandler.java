package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Bounded factual event-study read. It never asks the model to infer causality. */
@Component
public class MarketEventFactorObservatoryAgentToolHandler extends AbstractMarketResearchAgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.event_factor_observatory").name("Market event factor observatory").module("market")
            .description("Read a bounded limit-up or large-move cohort with multi-source factor evidence and explicit coverage.")
            .guidanceJson(MarketResearchAgentToolGuidance.FACTOR_OBSERVATORY)
            .inputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_OBSERVATORY)
            .outputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_OBSERVATORY_OUTPUT)
            .readOnly(true).requiresConfirmation(false).freshness("daily close plus materialized intraday coverage")
            .source(SOURCE).sourceUrl("/api/market/screener/factor-observatory").version("1.1.0").priority(69)
            .routeGroup("Agent / market").build();

    public MarketEventFactorObservatoryAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper) {
        super(client, objectMapper);
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String cohort = optionalString(call, "cohort", "LIMIT_UP").toUpperCase(Locale.ROOT);
        if (!"LIMIT_UP".equals(cohort) && !"LARGE_MOVE".equals(cohort)) {
            throw new IllegalArgumentException("cohort must be LIMIT_UP or LARGE_MOVE");
        }
        return query(DEFINITION, () -> client().getMarketEventFactorObservatory(
                date(call, "tradeDate"), cohort, decimal(call, "minLargeMovePct"),
                integer(call, "limit", 40, 1, 100), optionalString(call, "symbol", null)),
                "Read a bounded multi-factor market-event observation.");
    }

    private BigDecimal decimal(AgentToolCall call, String key) {
        Object value = arguments(call).get(key);
        if (value == null) return null;
        if (!(value instanceof Number)) throw new IllegalArgumentException(key + " must be a number");
        try {
            BigDecimal parsed = new BigDecimal(value.toString());
            if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(new BigDecimal("30")) > 0) {
                throw new IllegalArgumentException(key + " must be between 0 and 30");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a number", exception);
        }
    }
}
