package cn.cccs7.cynexus.ai.domain.gateway.quota;

import java.math.BigDecimal;

/** Durable owner-scoped quota boundary. */
public interface AgentQuotaPort {
    boolean reserveRequest(String ownerKey, String tenantKey, String principalKey, int estimatedTokens);

    void recordUsage(String ownerKey, String tenantKey, String principalKey, int reservedTokens,
                     int inputTokens, int outputTokens, int totalTokens, BigDecimal estimatedCost, String usageSource);
}
