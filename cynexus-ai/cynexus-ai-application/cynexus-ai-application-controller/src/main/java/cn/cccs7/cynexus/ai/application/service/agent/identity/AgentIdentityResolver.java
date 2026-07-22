package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentIdentityResolver {

    private final String secret;
    private final boolean allowAnonymous;

    public AgentIdentityResolver(
            @Value("${cynexus.ai.agent.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret,
            @Value("${cynexus.ai.agent.allow-anonymous-owner:${CYNEXUS_AI_AGENT_ALLOW_ANONYMOUS_OWNER:false}}") boolean allowAnonymous) {
        this.secret = secret;
        this.allowAnonymous = allowAnonymous;
    }

    public AgentIdentity resolve(String principal, String tenant, String signature) {
        if (TrustedPrincipalSigner.verify(principal, tenant, signature, secret)) {
            return AgentIdentity.builder().principalKey(principal.trim()).tenantKey(tenant.trim()).build();
        }
        if (allowAnonymous && isBlank(principal) && isBlank(tenant) && isBlank(signature)) {
            return AgentIdentity.singleUser();
        }
        throw new IllegalStateException("A trusted Agent identity is required");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
