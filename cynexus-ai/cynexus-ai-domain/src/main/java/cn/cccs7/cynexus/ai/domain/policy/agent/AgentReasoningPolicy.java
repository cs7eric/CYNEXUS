package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;

public final class AgentReasoningPolicy {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_EFFORTS = Set.of("low", "medium", "high");

    private AgentReasoningPolicy() {
    }

    public static String resolveRuntimeExtra(String providerExtra, String bindingParams, String requestedEffort) {
        String effort = normalize(requestedEffort);
        if (effort == null) effort = configuredEffort(bindingParams, AiConstants.REASONING_EFFORT_PARAM);
        if (effort == null) effort = configuredEffort(providerExtra, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        return withEffort(providerExtra, effort == null ? AiConstants.DEFAULT_REASONING_EFFORT : effort);
    }

    public static String applyOverride(String providerExtra, String requestedEffort) {
        String effort = normalize(requestedEffort);
        return effort == null ? providerExtra : withEffort(providerExtra, effort);
    }

    private static String withEffort(String providerExtra, String effort) {
        try {
            ObjectNode extra = providerExtra == null || providerExtra.isBlank()
                    ? OBJECT_MAPPER.createObjectNode()
                    : (ObjectNode) OBJECT_MAPPER.readTree(providerExtra);
            extra.put(AiConstants.MODEL_REASONING_EFFORT_EXTRA, effort);
            return OBJECT_MAPPER.writeValueAsString(extra);
        } catch (Exception exception) {
            throw new IllegalStateException("The AI provider extra configuration is invalid", exception);
        }
    }

    private static String configuredEffort(String json, String key) {
        if (json == null || json.isBlank()) return null;
        try {
            return normalize(OBJECT_MAPPER.readTree(json).path(key).asText(null));
        } catch (Exception exception) {
            throw new IllegalStateException("The AI reasoning configuration is invalid", exception);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EFFORTS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported reasoning effort: " + value);
        }
        return normalized;
    }
}
