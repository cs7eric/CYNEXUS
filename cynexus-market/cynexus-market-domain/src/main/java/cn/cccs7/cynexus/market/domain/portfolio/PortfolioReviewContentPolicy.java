package cn.cccs7.cynexus.market.domain.portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/** Normalizes user-entered review content before it reaches the JSONB columns. */
public final class PortfolioReviewContentPolicy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PortfolioReviewContentPolicy() {
    }

    public static String normalizeTags(String value) {
        if (value == null || value.isBlank()) {
            return "[]";
        }
        try {
            JsonNode parsed = MAPPER.readTree(value);
            if (parsed != null) {
                if (!parsed.isArray()) {
                    throw new IllegalArgumentException("portfolio review tags must be a JSON array");
                }
                return parsed.toString();
            }
        } catch (JsonProcessingException ignored) {
            // The review form accepts human-readable comma-separated tags.
        }

        ArrayNode tags = MAPPER.createArrayNode();
        for (String item : value.split("[,，、]")) {
            String tag = item.trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags.toString();
    }

    public static String normalizeMetricsSnapshot(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        try {
            JsonNode parsed = MAPPER.readTree(value);
            if (parsed != null && parsed.isObject()) {
                return parsed.toString();
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to a domain validation error instead of a database JSON error.
        }
        throw new IllegalArgumentException("portfolio review metrics snapshot must be a JSON object");
    }
}
