package cn.cccs7.cynexus.ai.domain.policy.sensitivedata;

import java.net.URI;
import java.util.Set;

public final class AgentSourceUrlPolicy {
    private AgentSourceUrlPolicy() {
    }

    /** Only app-relative paths or explicitly trusted HTTPS origins may be exposed as links. */
    public static String sanitize(String rawUrl, Set<String> trustedOrigins) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        String value = rawUrl.trim();
        if (value.chars().anyMatch(Character::isISOControl)) return null;
        if (value.startsWith("/") && !value.startsWith("//")) return value;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return null;
            String origin = "https://" + uri.getHost().toLowerCase()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            return trustedOrigins != null && trustedOrigins.stream()
                    .map(String::trim).map(String::toLowerCase).anyMatch(origin::equals) ? value : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
