package cn.cccs7.cynexus.ai.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerAgentObservabilityTest {
    @Test
    void recordsBoundedRunProviderAndToolMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerAgentObservability observability = new MicrometerAgentObservability(registry);
        String longToolCode = "tool-" + "x".repeat(200);

        observability.runStarted("trace-1");
        observability.runCompleted("SUCCEEDED", 12);
        observability.providerCompleted("xtyoo", "gpt-5.4-mini", true, 25);
        observability.toolCompleted(longToolCode, false, 7);

        assertThat(registry.counter("cynexus_ai_agent_runs_started").count()).isEqualTo(1.0);
        assertThat(registry.get("cynexus_ai_agent_run_duration").tag("status", "SUCCEEDED").timer().count()).isEqualTo(1L);
        assertThat(registry.get("cynexus_ai_agent_provider_calls")
                .tags("provider", "xtyoo", "model", "gpt-5.4-mini", "outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("cynexus_ai_agent_tool_calls")
                .tag("tool", longToolCode.substring(0, 80)).counter().count()).isEqualTo(1.0);
    }
}
