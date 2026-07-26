package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Reads provider usage variants without assuming one details object wins. */
public final class PromptCacheUsageParser {

    private PromptCacheUsageParser() {
    }

    public static PromptCacheUsage parse(JsonNode usage) {
        if (usage == null || !usage.isObject()) return PromptCacheUsage.builder().build();
        List<JsonNode> details = new ArrayList<>();
        addObject(details, usage.get(AiConstants.PROMPT_CACHE_USAGE_PRIMARY_DETAILS_FIELD));
        addObject(details, usage.get(AiConstants.PROMPT_CACHE_USAGE_ALTERNATIVE_DETAILS_FIELD));
        details.add(usage);
        return PromptCacheUsage.builder()
                .cachedInputTokens(maxInt(details,
                        AiConstants.PROMPT_CACHE_USAGE_CACHED_TOKENS_FIELD,
                        AiConstants.PROMPT_CACHE_USAGE_READ_TOKENS_FIELD,
                        AiConstants.PROMPT_CACHE_USAGE_HIT_TOKENS_FIELD,
                        AiConstants.PROMPT_CACHE_USAGE_CACHED_INPUT_TOKENS_FIELD))
                .cacheWriteTokens(maxInt(details,
                        AiConstants.PROMPT_CACHE_USAGE_WRITE_TOKENS_FIELD,
                        AiConstants.PROMPT_CACHE_USAGE_CREATION_TOKENS_FIELD))
                .totalCachedTokens(maxInt(List.of(usage),
                        AiConstants.PROMPT_CACHE_USAGE_TOTAL_CACHED_TOKENS_FIELD))
                .build();
    }

    private static void addObject(List<JsonNode> nodes, JsonNode node) {
        if (node != null && node.isObject()) nodes.add(node);
    }

    private static Integer maxInt(List<JsonNode> nodes, String... fields) {
        Integer maximum = null;
        for (JsonNode node : nodes) {
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && value.canConvertToInt()) {
                    int candidate = value.asInt();
                    maximum = maximum == null ? candidate : Math.max(maximum, candidate);
                }
            }
        }
        return maximum;
    }
}
