package cn.cccs7.cynexus.ai.domain.gateway.runtime;

/** Distributed circuit state for external Agent dependencies. */
public interface AgentCircuitBreakerPort {

    boolean allow(String dependencyKey);

    void recordSuccess(String dependencyKey);

    void recordFailure(String dependencyKey, int failureThreshold, int openSeconds);
}
