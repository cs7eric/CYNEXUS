package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reads a bounded strategy-health assessment using the Agent's signed identity only. */
@Component
public class StockSelectionStrategyHealthAgentToolHandler extends AbstractMarketResearchAgentToolHandler {

    private static final Set<String> STRATEGIES = Set.of(
            "TREND_MOMENTUM", "VOLUME_CAPITAL_RESONANCE", "INDUSTRY_RELATIVE_STRENGTH");
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.stock_selection_strategy_health").name("Stock selection strategy health").module("market")
            .description("Read a bounded owner-scoped health assessment for one governed stock-selection strategy.")
            .guidanceJson(MarketResearchAgentToolGuidance.STOCK_SELECTION_STRATEGY_HEALTH)
            .inputSchemaJson(MarketResearchAgentToolSchemas.STOCK_SELECTION_STRATEGY_HEALTH)
            .outputSchemaJson(MarketResearchAgentToolSchemas.STOCK_SELECTION_STRATEGY_HEALTH_OUTPUT)
            .readOnly(true).requiresConfirmation(false).freshness("daily-close strategy health and latest completed backtest evidence")
            .source(SOURCE).sourceUrl("/api/market/screener/strategies/{strategyCode}/health").version("1.0.0").priority(72)
            .routeGroup("Agent / market").build();

    private final String trustedIdentitySecret;

    public StockSelectionStrategyHealthAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper,
                                                         @Value("${cynexus.ai.agent.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String trustedIdentitySecret) {
        super(client, objectMapper);
        this.trustedIdentitySecret = trustedIdentitySecret;
    }

    @Override
    public AgentToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        if (context == null || blank(context.getOwnerKey()) || blank(context.getTenantKey())) {
            return AgentToolResult.failure(DEFINITION.getCode(), "A trusted Agent identity is required for strategy health.",
                    DEFINITION.getSource(), "TRUSTED_IDENTITY_REQUIRED");
        }
        if (blank(trustedIdentitySecret)) {
            return AgentToolResult.failure(DEFINITION.getCode(), "The scoped strategy-health query is unavailable.",
                    DEFINITION.getSource(), "TRUSTED_IDENTITY_UNAVAILABLE");
        }
        String strategyCode = requiredString(call, "strategyCode").toUpperCase(Locale.ROOT);
        if (!STRATEGIES.contains(strategyCode)) {
            throw new IllegalArgumentException("strategyCode is not a published governed strategy");
        }
        String signature = TrustedPrincipalSigner.sign(context.getOwnerKey(), context.getTenantKey(), trustedIdentitySecret);
        return query(DEFINITION, () -> client().getStockSelectionStrategyHealth(
                strategyCode, date(call, "tradeDate"), context.getOwnerKey(), context.getTenantKey(), signature),
                "Read the active user's bounded stock-selection strategy health.");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
