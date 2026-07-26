package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import java.util.Map;

public final class OpenAiPromptCacheStrategy implements PromptCacheStrategy {

    @Override
    public boolean supports(AiModelBO model) {
        return provider(model, "openai");
    }

    @Override
    public void enrichPayload(AiModelBO model, PromptCacheContext context,
                              Map<String, Object> payload, boolean responsesApi) {
        if (context != null && context.getStablePrefixKey() != null
                && !context.getStablePrefixKey().isBlank()) {
            payload.put(AiConstants.PROMPT_CACHE_KEY_FIELD, context.getStablePrefixKey());
        }
    }

    private boolean provider(AiModelBO model, String expected) {
        return model != null && (expected.equalsIgnoreCase(value(model.getProviderIdentity()))
                || expected.equalsIgnoreCase(value(model.getProviderCode())));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
