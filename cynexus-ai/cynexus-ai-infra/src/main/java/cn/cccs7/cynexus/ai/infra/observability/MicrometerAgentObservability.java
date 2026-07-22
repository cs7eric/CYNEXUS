package cn.cccs7.cynexus.ai.infra.observability;

import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class MicrometerAgentObservability implements AgentObservabilityPort {
    private final MeterRegistry registry;

    public MicrometerAgentObservability(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override public void runStarted(String traceId) {
        registry.counter("cynexus_ai_agent_runs_started").increment();
    }

    @Override public void runCompleted(String status, long durationMs) {
        registry.counter("cynexus_ai_agent_runs_completed", "status", safe(status)).increment();
        registry.timer("cynexus_ai_agent_run_duration", "status", safe(status)).record(Math.max(0, durationMs), TimeUnit.MILLISECONDS);
    }

    @Override public void providerCompleted(String provider, String model, boolean success, long durationMs) {
        registry.counter("cynexus_ai_agent_provider_calls", "provider", safe(provider), "model", safe(model), "outcome", success ? "success" : "failure").increment();
        registry.timer("cynexus_ai_agent_provider_latency", "provider", safe(provider), "model", safe(model)).record(Math.max(0, durationMs), TimeUnit.MILLISECONDS);
    }

    @Override public void toolCompleted(String toolCode, boolean success, long durationMs) {
        registry.counter("cynexus_ai_agent_tool_calls", "tool", safe(toolCode), "outcome", success ? "success" : "failure").increment();
        registry.timer("cynexus_ai_agent_tool_latency", "tool", safe(toolCode)).record(Math.max(0, durationMs), TimeUnit.MILLISECONDS);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.length() > 80 ? value.substring(0, 80) : value;
    }
}
