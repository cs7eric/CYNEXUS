package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import java.util.Map;

/**
 * Applies the OpenAI-compatible prompt cache contract to every model routed
 * through the generic chat adapter. The upstream provider may still ignore
 * the hint, but the request is cache-enabled whenever that contract exists.
 */
public final class GenericPromptCacheStrategy implements PromptCacheStrategy {

    @Override
    public boolean supports(AiModelBO model) {
        return true;
    }

    @Override
    public void enrichPayload(AiModelBO model, PromptCacheContext context,
                              Map<String, Object> payload, boolean responsesApi) {
        if (context != null && context.getStablePrefixKey() != null
                && !context.getStablePrefixKey().isBlank()) {
            payload.put(AiConstants.PROMPT_CACHE_KEY_FIELD, context.getStablePrefixKey());
        }
    }

}
