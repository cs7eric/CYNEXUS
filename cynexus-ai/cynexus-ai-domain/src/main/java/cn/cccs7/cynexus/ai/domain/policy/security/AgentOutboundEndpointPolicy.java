package cn.cccs7.cynexus.ai.domain.policy.security;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates destinations used by AI integrations before infrastructure opens a connection.
 * Origins are exact matches; wildcards and credentials in URLs are intentionally unsupported.
 */
public final class AgentOutboundEndpointPolicy {
    private AgentOutboundEndpointPolicy() {
    }

    public static URI requireAllowed(String rawUrl, String configuredOrigins) {
        URI candidate = parse(rawUrl);
        Set<String> allowedOrigins = parseOrigins(configuredOrigins);
        String origin = origin(candidate);
        if (!allowedOrigins.contains(origin)) {
            throw new IllegalArgumentException("AI outbound endpoint is not allow-listed: " + origin);
        }
        return candidate;
    }

    public static boolean isAllowed(String rawUrl, String configuredOrigins) {
        try {
            requireAllowed(rawUrl, configuredOrigins);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static String origin(URI uri) {
        int port = uri.getPort();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://"
                + uri.getHost().toLowerCase(Locale.ROOT)
                + (port > 0 ? ":" + port : "");
    }

    private static URI parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("AI outbound endpoint is blank");
        }
        String value = rawUrl.trim();
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("AI outbound endpoint contains control characters");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AI outbound endpoint is malformed", exception);
        }
        String scheme = uri.getScheme();
        if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("AI outbound endpoint must use HTTP(S)");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("AI outbound endpoint must be an origin with an optional path");
        }
        return uri;
    }

    private static Set<String> parseOrigins(String configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isBlank()) return Set.of();
        return Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AgentOutboundEndpointPolicy::parse)
                .map(AgentOutboundEndpointPolicy::origin)
                .collect(Collectors.toUnmodifiableSet());
    }
}
