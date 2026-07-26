package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AgentReasoningPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void appliesBindingWireApiAlongsideReasoningEffort() throws Exception {
        String extra = AgentReasoningPolicy.resolveRuntimeExtra(
                "{}", "{\"reasoning_effort\":\"low\",\"wire_api\":\"responses\"}", null);

        var json = objectMapper.readTree(extra);
        assertThat(json.path("model_reasoning_effort").asText()).isEqualTo("low");
        assertThat(json.path("wire_api").asText()).isEqualTo("responses");
    }

    @Test
    void bindingWireApiOverridesProviderDefault() throws Exception {
        String extra = AgentReasoningPolicy.resolveRuntimeExtra(
                "{\"wire_api\":\"chat_completions\"}",
                "{\"wire_api\":\"responses\"}", null);

        assertThat(objectMapper.readTree(extra).path("wire_api").asText()).isEqualTo("responses");
    }
}
