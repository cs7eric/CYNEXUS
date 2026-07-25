package cn.cccs7.cynexus.ai.domain.policy.sensitivedata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Defines the field names that must never cross the Agent audit/model boundary. */
public final class AgentSensitiveDataPolicy {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "secret", "token", "accesstoken", "refreshtoken", "apikey",
            "authorization", "cookie", "setcookie", "privatekey", "accesskey", "phone", "mobile",
            "email", "idcard", "identitycard", "bankaccount", "creditcard"
    );

    private AgentSensitiveDataPolicy() {
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) return false;
        String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }

    public static String marker() {
        return "[REDACTED]";
    }

    public static String redactText(String value) {
        if (value == null || value.isBlank()) return value;
        return value
                .replaceAll("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+", "$1" + marker())
                .replaceAll("(?i)(api[-_ ]?key\\s*[:=]\\s*)[^\\s,;]+", "$1" + marker())
                .replaceAll("(?i)(secret\\s*[:=]\\s*)[^\\s,;]+", "$1" + marker())
                .replaceAll("\\bsk-[A-Za-z0-9]{16,}\\b", marker())
                .replaceAll("\\b(?:ghp|github_pat)_[A-Za-z0-9_]{20,}\\b", marker())
                .replaceAll("\\bAKIA[0-9A-Z]{16}\\b", marker())
                .replaceAll("(?<![A-Za-z0-9])eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+(?![A-Za-z0-9])", marker())
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", marker())
                .replaceAll("(?<!\\d)(?:\\+?86[- ]?)?1[3-9]\\d{9}(?!\\d)", marker());
    }

    public static String redactJson(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            JsonNode node = MAPPER.readTree(value);
            redactNode(node);
            return MAPPER.writeValueAsString(node);
        } catch (Exception ignored) {
            return redactText(value);
        }
    }

    public static void redactNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveKey(entry.getKey())) {
                    ((ObjectNode) node).put(entry.getKey(), marker());
                } else {
                    redactNode(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                redactNode(item);
            }
        }
    }
}