package cn.cccs7.cynexus.ai.application.service.agent.identity;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentGovernancePolicy;
import cn.cccs7.cynexus.common.security.identity.TrustedPrincipalSigner;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentIdentityResolver {

    private final String secret;
    private final boolean allowAnonymous;
    private final Set<String> governancePrincipals;

    public AgentIdentityResolver(
            @Value("${cynexus.ai.agent.trusted-identity-secret:${CYNEXUS_TRUSTED_IDENTITY_SECRET:}}") String secret,
            @Value("${cynexus.ai.agent.allow-anonymous-owner:${CYNEXUS_AI_AGENT_ALLOW_ANONYMOUS_OWNER:false}}") boolean allowAnonymous,
            @Value("${cynexus.ai.agent.governance-principals:${CYNEXUS_AI_AGENT_GOVERNANCE_PRINCIPALS:workspace-user}}") String governancePrincipals) {
        this.secret = secret;
        this.allowAnonymous = allowAnonymous;
        this.governancePrincipals = Arrays.stream((governancePrincipals == null ? "" : governancePrincipals).split(","))
                .map(String::trim).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public AgentIdentityResolver(String secret, boolean allowAnonymous) {
        this(secret, allowAnonymous, "workspace-user");
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

    public AgentIdentity resolveGovernance(String principal, String tenant, String signature) {
        AgentIdentity identity = resolve(principal, tenant, signature);
        AgentGovernancePolicy.requireAdministrator(identity, governancePrincipals);
        return identity;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
