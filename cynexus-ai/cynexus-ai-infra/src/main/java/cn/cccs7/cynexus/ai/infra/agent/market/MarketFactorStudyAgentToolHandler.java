package cn.cccs7.cynexus.ai.infra.agent.market;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionContext;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import cn.cccs7.cynexus.market.api.feign.MarketResearchFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Strict owner-scoped read of durable market-factor study metadata. */
@Component
public class MarketFactorStudyAgentToolHandler extends AbstractMarketResearchAgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.factor_study").name("Market factor studies").module("market")
            .description("Read bounded persisted market-factor study status, frozen time window, leakage result and coverage for the current owner.")
            .guidanceJson(MarketResearchAgentToolGuidance.FACTOR_STUDY).inputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_STUDY)
            .outputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_STUDY_OUTPUT).readOnly(true).requiresConfirmation(false)
            .freshness("persisted study metadata at query time").source(SOURCE).sourceUrl("/api/market/screener/factor-research/studies")
            .version("1.0.0").priority(70).routeGroup("Agent / market").build();
    private final String secret;
    public MarketFactorStudyAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper,
            @Value("${cynexus.ai.agent.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret) { super(client, objectMapper); this.secret = secret; }
    @Override public AgentToolDefinition definition() { return DEFINITION; }
    @Override public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        if (context == null || blank(context.getOwnerKey()) || blank(context.getTenantKey()) || blank(secret)) {
            return AgentToolResult.failure(DEFINITION.getCode(), "A trusted Agent identity is required for factor-study reads.", DEFINITION.getSource(), "TRUSTED_IDENTITY_REQUIRED");
        }
        String signature = TrustedPrincipalSigner.sign(context.getOwnerKey(), context.getTenantKey(), secret);
        return query(DEFINITION, () -> client().getMarketFactorStudies(integer(call, "offset", 0, 0, 200), integer(call, "limit", 20, 1, 50), context.getOwnerKey(), context.getTenantKey(), signature),
                "Read bounded owner-scoped factor-study metadata.");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
