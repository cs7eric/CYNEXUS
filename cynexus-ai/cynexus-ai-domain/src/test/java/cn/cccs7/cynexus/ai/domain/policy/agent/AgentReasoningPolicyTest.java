package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void mergesOnlyValidatedResponsesCacheRuntimeParams() throws Exception {
        String extra = AgentReasoningPolicy.resolveRuntimeExtra(
                "{\"provider_setting\":\"kept\"}",
                "{\"wire_api\":\"responses\",\"responses_state_mode\":\"previous_response_id\","
                        + "\"disable_response_storage\":false,\"prompt_cache_retention\":\"24h\","
                        + "\"uncontrolled_field\":\"ignored\"}", null);

        var json = objectMapper.readTree(extra);
        assertThat(json.path("provider_setting").asText()).isEqualTo("kept");
        assertThat(json.path("responses_state_mode").asText()).isEqualTo("previous_response_id");
        assertThat(json.path("disable_response_storage").asBoolean()).isFalse();
        assertThat(json.path("prompt_cache_retention").asText()).isEqualTo("24h");
        assertThat(json.has("uncontrolled_field")).isFalse();
    }

    @Test
    void rejectsInvalidResponsesCacheRuntimeParamTypes() {
        assertThatThrownBy(() -> AgentReasoningPolicy.resolveRuntimeExtra(
                "{}", "{\"disable_response_storage\":\"false\"}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disable_response_storage");
    }
}
