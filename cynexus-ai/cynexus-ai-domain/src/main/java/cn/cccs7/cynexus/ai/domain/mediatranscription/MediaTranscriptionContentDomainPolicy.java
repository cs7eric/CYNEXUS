package cn.cccs7.cynexus.ai.domain.mediatranscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public final class MediaTranscriptionContentDomainPolicy {

    public static final int MAX_INPUT_LENGTH = 60_000;
    private final ObjectMapper mapper = new ObjectMapper();

    public String normalize(String value) { return value == null ? "" : value.trim(); }

    public String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    public String blankTo(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    public MetadataDraft parseMetadata(String extractedJson) {
        try {
            JsonNode root = mapper.readTree(extractedJson == null ? "{}" : extractedJson);
            return new MetadataDraft(
                    blankTo(root.path("title").asText(null), "未命名笔记"),
                    readStringList(root.get("categories"), List.of("未分类")),
                    readStringList(root.get("tags"), List.of("待整理")));
        } catch (Exception ignored) {
            return fallbackMetadata();
        }
    }

    public MetadataDraft fallbackMetadata() {
        return new MetadataDraft("未命名笔记", List.of("未分类"), List.of("待整理"));
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) return fallback;
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = normalize(item.asText(""));
            if (!value.isBlank()) values.add(value);
        }
        return values.isEmpty() ? fallback : values;
    }

    public record MetadataDraft(String title, List<String> categories, List<String> tags) { }
}
