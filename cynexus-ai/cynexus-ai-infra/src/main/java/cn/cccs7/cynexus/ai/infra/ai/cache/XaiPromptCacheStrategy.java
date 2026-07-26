package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import java.util.Map;

public final class XaiPromptCacheStrategy implements PromptCacheStrategy {

    @Override
    public boolean supports(AiModelBO model) {
        return provider(model, "xai") || provider(model, "grok");
    }

    @Override
    public Map<String, String> headers(AiModelBO model, PromptCacheContext context) {
        if (context == null || context.getConversationKey() == null || context.getConversationKey().isBlank()) {
            return Map.of();
        }
        return Map.of(AiConstants.PROMPT_CACHE_XAI_CONVERSATION_HEADER, context.getConversationKey());
    }

    @Override
    public void enrichPayload(AiModelBO model, PromptCacheContext context,
                              Map<String, Object> payload, boolean responsesApi) {
        if (!responsesApi || context == null || context.getRoutingKey() == null
                || context.getRoutingKey().isBlank()) return;
        payload.put(AiConstants.PROMPT_CACHE_KEY_FIELD, context.getRoutingKey());
    }

    private boolean provider(AiModelBO model, String expected) {
        return model != null && (expected.equalsIgnoreCase(value(model.getProviderIdentity()))
                || expected.equalsIgnoreCase(value(model.getProviderCode())));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
