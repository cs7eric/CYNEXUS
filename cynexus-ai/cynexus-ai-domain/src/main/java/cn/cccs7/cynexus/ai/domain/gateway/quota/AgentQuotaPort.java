package cn.cccs7.cynexus.ai.domain.gateway.quota;

import java.math.BigDecimal;

/** Durable owner-scoped quota boundary. Tests and non-persistent adapters may use the defaults. */
public interface AgentQuotaPort {
    default boolean reserveRequest(String ownerKey, String tenantKey, String principalKey, int estimatedTokens) { return true; }
    default void recordUsage(String ownerKey, String tenantKey, String principalKey, int reservedTokens,
                             int inputTokens, int outputTokens, int totalTokens, BigDecimal estimatedCost, String usageSource) {
    }
}
