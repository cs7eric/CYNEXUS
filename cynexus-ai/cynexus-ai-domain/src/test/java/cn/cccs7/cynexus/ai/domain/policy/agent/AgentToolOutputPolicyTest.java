package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentToolOutputPolicyTest {
    @Test
    void truncatesLargeOutput() {
        String raw = "{\"apiKey\":\"secret-value\",\"email\":\"person@example.com\",\"payload\":\"" + "x".repeat(25_000) + "\"}";
        AgentToolResult result = AgentToolResult.success("market.overview", raw, "Market data", "2026-07-22", "Market API", List.of());
        AgentToolResult bounded = AgentToolOutputPolicy.bound(result, 24_000);
        assertThat(bounded.getOutputJson()).contains("truncated");
        assertThat(bounded.getOutputJson().length()).isLessThan(raw.length());
    }
}
