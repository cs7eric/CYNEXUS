package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface PromptCacheStrategy {

    boolean supports(AiModelBO model);

    default Map<String, String> headers(AiModelBO model, PromptCacheContext context) {
        return Map.of();
    }

    default void enrichPayload(AiModelBO model, PromptCacheContext context,
                               Map<String, Object> payload, boolean responsesApi) {
    }

    default PromptCacheUsage extractUsage(JsonNode usage) {
        return PromptCacheUsageParser.parse(usage);
    }
}
