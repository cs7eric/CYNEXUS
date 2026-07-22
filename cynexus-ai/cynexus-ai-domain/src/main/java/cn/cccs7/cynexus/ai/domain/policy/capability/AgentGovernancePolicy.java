package cn.cccs7.cynexus.ai.domain.policy.capability;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import java.util.Set;

/** Authorization rule for capability catalog and governance administration. */
public final class AgentGovernancePolicy {
    private AgentGovernancePolicy() {
    }

    public static void requireAdministrator(AgentIdentity identity, Set<String> allowedPrincipals) {
        if (identity == null || allowedPrincipals == null || allowedPrincipals.isEmpty()
                || !allowedPrincipals.contains(identity.getPrincipalKey())) {
            throw new IllegalStateException("Agent capability governance requires an administrator identity");
        }
    }
}
