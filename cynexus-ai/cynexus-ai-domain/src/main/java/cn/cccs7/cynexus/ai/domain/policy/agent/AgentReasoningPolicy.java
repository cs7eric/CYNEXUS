package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;

public final class AgentReasoningPolicy {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_EFFORTS = Set.of("low", "medium", "high");
    private static final Set<String> SUPPORTED_WIRE_APIS = Set.of(
            AiConstants.WIRE_API_RESPONSES, AiConstants.WIRE_API_CHAT_COMPLETIONS);
    private static final Set<String> SUPPORTED_RESPONSE_STATE_MODES = Set.of(
            AiConstants.RESPONSES_STATE_MODE_PREVIOUS_RESPONSE_ID);
    private static final Set<String> SUPPORTED_CACHE_RETENTIONS = Set.of(
            AiConstants.PROMPT_CACHE_RETENTION_IN_MEMORY, AiConstants.PROMPT_CACHE_RETENTION_24_HOURS);

    private AgentReasoningPolicy() {
    }

    public static String resolveRuntimeExtra(String providerExtra, String bindingParams, String requestedEffort) {
        String effort = normalize(requestedEffort);
        if (effort == null) effort = configuredEffort(bindingParams, AiConstants.REASONING_EFFORT_PARAM);
        if (effort == null) effort = configuredEffort(providerExtra, AiConstants.MODEL_REASONING_EFFORT_EXTRA);
        String resolvedExtra = mergeBindingRuntimeParams(providerExtra, bindingParams);
        return withEffort(resolvedExtra, effort == null ? AiConstants.DEFAULT_REASONING_EFFORT : effort);
    }

    public static String applyOverride(String providerExtra, String requestedEffort) {
        String effort = normalize(requestedEffort);
        return effort == null ? providerExtra : withEffort(providerExtra, effort);
    }

    private static String withEffort(String providerExtra, String effort) {
        return withValue(providerExtra, AiConstants.MODEL_REASONING_EFFORT_EXTRA, effort);
    }

    private static String withValue(String providerExtra, String key, String value) {
        try {
            ObjectNode extra = providerExtra == null || providerExtra.isBlank()
                    ? OBJECT_MAPPER.createObjectNode()
                    : (ObjectNode) OBJECT_MAPPER.readTree(providerExtra);
            extra.put(key, value);
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

    private static String mergeBindingRuntimeParams(String providerExtra, String bindingParams) {
        try {
            ObjectNode extra = objectNode(providerExtra, "provider extra");
            if (bindingParams == null || bindingParams.isBlank()) {
                return OBJECT_MAPPER.writeValueAsString(extra);
            }
            JsonNode params = OBJECT_MAPPER.readTree(bindingParams);
            if (!params.isObject()) throw new IllegalArgumentException("AI binding params must be a JSON object");
            copyText(params, extra, AiConstants.MODEL_WIRE_API_EXTRA, SUPPORTED_WIRE_APIS);
            copyText(params, extra, AiConstants.RESPONSES_STATE_MODE_EXTRA, SUPPORTED_RESPONSE_STATE_MODES);
            copyBoolean(params, extra, AiConstants.RESPONSES_DISABLE_STORAGE_EXTRA);
            copyText(params, extra, AiConstants.PROMPT_CACHE_RETENTION_EXTRA, SUPPORTED_CACHE_RETENTIONS);
            return OBJECT_MAPPER.writeValueAsString(extra);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("The AI runtime configuration is invalid", exception);
        }
    }

    private static ObjectNode objectNode(String json, String fieldName) throws Exception {
        if (json == null || json.isBlank()) return OBJECT_MAPPER.createObjectNode();
        JsonNode value = OBJECT_MAPPER.readTree(json);
        if (!value.isObject()) throw new IllegalArgumentException("AI " + fieldName + " must be a JSON object");
        return (ObjectNode) value;
    }

    private static void copyText(JsonNode source, ObjectNode target, String key, Set<String> supportedValues) {
        JsonNode value = source.get(key);
        if (value == null || value.isNull()) return;
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI binding param " + key + " must be a non-blank string");
        }
        String normalized = value.asText().trim().toLowerCase(Locale.ROOT);
        if (!supportedValues.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported AI binding param " + key + ": " + value.asText());
        }
        target.put(key, normalized);
    }

    private static void copyBoolean(JsonNode source, ObjectNode target, String key) {
        JsonNode value = source.get(key);
        if (value == null || value.isNull()) return;
        if (!value.isBoolean()) throw new IllegalArgumentException("AI binding param " + key + " must be boolean");
        target.put(key, value.asBoolean());
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
