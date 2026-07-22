package cn.cccs7.cynexus.ai.domain.agent;

public interface AgentRateLimitPort {

    boolean allow(String ownerKey, int maxRequests, int windowSeconds);
}
