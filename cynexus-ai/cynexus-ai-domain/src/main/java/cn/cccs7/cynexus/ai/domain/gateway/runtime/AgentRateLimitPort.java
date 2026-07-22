package cn.cccs7.cynexus.ai.domain.gateway.runtime;

public interface AgentRateLimitPort {

    boolean allow(String ownerKey, int maxRequests, int windowSeconds);
}
