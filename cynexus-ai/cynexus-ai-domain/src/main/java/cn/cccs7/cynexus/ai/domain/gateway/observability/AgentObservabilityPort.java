package cn.cccs7.cynexus.ai.domain.gateway.observability;

/** Application-facing metrics boundary; implementations stay in infrastructure. */
public interface AgentObservabilityPort {
    void runStarted(String traceId);

    void runCompleted(String status, long durationMs);

    void providerCompleted(String provider, String model, boolean success, long durationMs);

    void toolCompleted(String toolCode, boolean success, long durationMs);
}
