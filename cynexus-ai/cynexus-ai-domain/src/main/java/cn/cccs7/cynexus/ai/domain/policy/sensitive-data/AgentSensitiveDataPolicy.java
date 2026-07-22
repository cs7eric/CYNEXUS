package cn.cccs7.cynexus.ai.domain.agent;

import java.util.Locale;
import java.util.Set;

/** Defines the field names that must never cross the Agent audit/model boundary. */
public final class AgentSensitiveDataPolicy {
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
}
