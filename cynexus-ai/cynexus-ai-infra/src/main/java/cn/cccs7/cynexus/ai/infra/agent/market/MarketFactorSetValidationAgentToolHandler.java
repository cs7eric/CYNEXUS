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

/** Bounded owner-scoped validation evidence read, deliberately not a promotion command. */
@Component
public class MarketFactorSetValidationAgentToolHandler extends AbstractMarketResearchAgentToolHandler {
    private static final AgentToolDefinition DEFINITION = AgentToolDefinition.builder()
            .code("market.factor_set_validation").name("Market Factor Set validation").module("market")
            .description("Read validation and active-version status for one owner-scoped Market Factor Set.")
            .guidanceJson(MarketResearchAgentToolGuidance.FACTOR_SET_VALIDATION).inputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_SET_VALIDATION)
            .outputSchemaJson(MarketResearchAgentToolSchemas.FACTOR_SET_VALIDATION_OUTPUT).readOnly(true).requiresConfirmation(false)
            .freshness("persisted Factor Set validation at query time").source(SOURCE).sourceUrl("/api/market/screener/factor-research/factor-sets/{code}")
            .version("1.0.0").priority(71).routeGroup("Agent / market").build();
    private final String secret;
    public MarketFactorSetValidationAgentToolHandler(MarketResearchFeignClient client, ObjectMapper objectMapper,
            @Value("${cynexus.ai.agent.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret) { super(client, objectMapper); this.secret = secret; }
    @Override public AgentToolDefinition definition() { return DEFINITION; }
    @Override public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        if (context == null || blank(context.getOwnerKey()) || blank(context.getTenantKey()) || blank(secret)) return AgentToolResult.failure(DEFINITION.getCode(), "A trusted Agent identity is required for Factor Set reads.", DEFINITION.getSource(), "TRUSTED_IDENTITY_REQUIRED");
        String code = requiredString(call, "code"); if (!code.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("code format is invalid");
        String signature = TrustedPrincipalSigner.sign(context.getOwnerKey(), context.getTenantKey(), secret);
        return query(DEFINITION, () -> client().getMarketFactorSet(code, context.getOwnerKey(), context.getTenantKey(), signature), "Read owner-scoped Factor Set validation evidence.");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
