package cn.cccs7.cynexus.ai.domain.policy.quota;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;

public final class AgentQuotaPolicy {
    private AgentQuotaPolicy() { }

    public static void validate(AgentQuotaPolicyBO policy) {
        if (policy == null) throw new IllegalArgumentException("Quota policy is required");
        if (policy.getTenantKey() == null && policy.getPrincipalKey() != null) {
            throw new IllegalArgumentException("A principal policy must specify a tenant");
        }
        if (policy.getDailyRequestLimit() == null || policy.getDailyRequestLimit() < 1) {
            throw new IllegalArgumentException("Daily request limit must be positive");
        }
        if (policy.getDailyTokenLimit() == null || policy.getDailyTokenLimit() < 1024) {
            throw new IllegalArgumentException("Daily token limit must be at least 1024");
        }
        if (policy.getTenantKey() != null && policy.getTenantKey().length() > 128) {
            throw new IllegalArgumentException("Tenant key is too long");
        }
        if (policy.getPrincipalKey() != null && policy.getPrincipalKey().length() > 128) {
            throw new IllegalArgumentException("Principal key is too long");
        }
    }
}
