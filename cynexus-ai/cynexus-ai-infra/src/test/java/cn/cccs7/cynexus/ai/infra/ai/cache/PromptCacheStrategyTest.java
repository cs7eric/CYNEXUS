package cn.cccs7.cynexus.ai.infra.ai.cache;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptCacheStrategyTest {

    private final PromptCacheContext context = PromptCacheContext.builder()
            .conversationKey("cynexus-conversation-test")
            .stablePrefixKey("cynexus-prefix-test")
            .stablePrefixVersion("v1")
            .build();

    @Test
    void enrichesOpenAiPayloadAndReadsNestedUsage() throws Exception {
        OpenAiPromptCacheStrategy strategy = new OpenAiPromptCacheStrategy();
        AiModelBO model = AiModelBO.builder().providerIdentity("openai").providerCode("openai").build();
        Map<String, Object> payload = new LinkedHashMap<>();

        strategy.enrichPayload(model, context, payload, false);

        assertThat(payload).containsEntry("prompt_cache_key", "cynexus-prefix-test");
        PromptCacheUsage usage = strategy.extractUsage(new ObjectMapper().readTree(
                "{\"prompt_tokens_details\":{\"cached_tokens\":42,\"cache_write_tokens\":9}}"));
        assertThat(usage.getCachedInputTokens()).isEqualTo(42);
        assertThat(usage.getCacheWriteTokens()).isEqualTo(9);
    }

    @Test
    void addsXaiConversationHeaderForChatAndPrefixKeyForResponses() throws Exception {
        XaiPromptCacheStrategy strategy = new XaiPromptCacheStrategy();
        AiModelBO model = AiModelBO.builder().providerIdentity("grok").providerCode("custom").build();

        assertThat(strategy.headers(model, context)).containsEntry("x-grok-conv-id", "cynexus-conversation-test");
        Map<String, Object> payload = new LinkedHashMap<>();
        strategy.enrichPayload(model, context, payload, true);
        assertThat(payload).containsEntry("prompt_cache_key", "cynexus-prefix-test");
    }

    @Test
    void enablesPromptCacheForEveryOpenAiCompatibleProvider() {
        PromptCacheStrategy strategy = PromptCacheStrategyRegistry.defaultRegistry().resolve(
                AiModelBO.builder().providerIdentity("xhhai").providerCode("xhhai").build());
        Map<String, Object> payload = new LinkedHashMap<>();

        strategy.enrichPayload(null, context, payload, false);

        assertThat(strategy).isInstanceOf(GenericPromptCacheStrategy.class);
        assertThat(strategy.headers(null, context)).isEmpty();
        assertThat(payload).containsEntry("prompt_cache_key", "cynexus-prefix-test");
    }

    @Test
    void readsTheLargestValueAcrossAlternativeUsageDetails() throws Exception {
        PromptCacheUsage usage = new GenericPromptCacheStrategy().extractUsage(new ObjectMapper().readTree(
                "{\"prompt_tokens_details\":{\"cached_tokens\":0},"
                        + "\"input_tokens_details\":{\"cache_read_input_tokens\":128,"
                        + "\"cache_creation_input_tokens\":64}}"));

        assertThat(usage.getCachedInputTokens()).isEqualTo(128);
        assertThat(usage.getCacheWriteTokens()).isEqualTo(64);
    }

    @Test
    void readsFlatStreamingUsageFields() throws Exception {
        PromptCacheUsage usage = new GenericPromptCacheStrategy().extractUsage(new ObjectMapper().readTree(
                "{\"cached_input_tokens\":512,\"cache_write_tokens\":32}"));

        assertThat(usage.getCachedInputTokens()).isEqualTo(512);
        assertThat(usage.getCacheWriteTokens()).isEqualTo(32);
    }
}
