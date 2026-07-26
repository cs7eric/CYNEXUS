package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolDefinition;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.PromptCacheContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Builds stable, non-sensitive cache routing keys without exposing tenant identifiers. */
public final class AgentPromptCacheKeyPolicy {

    private AgentPromptCacheKeyPolicy() {
    }

    public static PromptCacheContext context(String ownerKey, String conversationNo, String stableSystemPrompt,
                                             AiModelBO model, List<AgentToolDefinition> tools) {
        String conversationKey = digest(AiConstants.PROMPT_CACHE_KEY_VERSION + "|conversation|"
                + value(ownerKey) + "|" + value(conversationNo));
        String stablePrefix = new StringBuilder()
                .append(AiConstants.PROMPT_CACHE_KEY_VERSION).append('|')
                .append(AiConstants.PROMPT_CACHE_CAPABILITY).append('|')
                .append(value(model == null ? null : model.getProviderIdentity())).append('|')
                .append(value(model == null ? null : model.getModelCode())).append('|')
                .append(value(stableSystemPrompt)).append('|')
                .append(canonicalTools(tools))
                .toString();
        String stablePrefixKey = AiConstants.PROMPT_CACHE_STABLE_PREFIX_KEY_PREFIX + digest(stablePrefix);
        int routingShard = (int) (Long.parseLong(conversationKey.substring(0, 8), 16)
                % AiConstants.PROMPT_CACHE_ROUTING_SHARD_COUNT);
        String routingKey = AiConstants.PROMPT_CACHE_KEY_VERSION + "|routing|"
                + stablePrefixKey + "|conversation|" + conversationKey + "|shard-" + routingShard;
        return PromptCacheContext.builder()
                .conversationKey(AiConstants.PROMPT_CACHE_CONVERSATION_KEY_PREFIX + conversationKey)
                .stablePrefixKey(stablePrefixKey)
                .routingKey(AiConstants.PROMPT_CACHE_ROUTING_KEY_PREFIX + digest(routingKey))
                .stablePrefixVersion(AiConstants.PROMPT_CACHE_KEY_VERSION)
                .build();
    }

    private static String canonicalTools(List<AgentToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) return "none";
        return tools.stream()
                .filter(tool -> tool != null)
                .sorted(Comparator.comparing(tool -> value(tool.getCode())))
                .map(tool -> String.join("|",
                        value(tool.getCode()), value(tool.getDescription()), value(tool.getGuidanceJson()),
                        value(tool.getInputSchemaJson()), value(tool.getOutputSchemaJson())))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("none");
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
