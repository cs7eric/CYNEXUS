package cn.cccs7.cynexus.ai.domain.port;

/** Application-facing metrics boundary; implementations stay in infrastructure. */
public interface AgentObservabilityPort {
    default void runStarted(String traceId) { }
    default void runCompleted(String status, long durationMs) { }
    default void providerCompleted(String provider, String model, boolean success, long durationMs) { }
    default void toolCompleted(String toolCode, boolean success, long durationMs) { }
}
