package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ApplicationAnalysisPageDomainPolicy {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String normalizeTargetKey(String value) {
        return defaultString(value, "").toLowerCase();
    }

    public String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String buildTargetKey(String targetType, String symbol) {
        return normalizeTargetKey(defaultString(targetType, "stock") + "-" + defaultString(symbol, ""));
    }

    public LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    public List<String> readStringList(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(defaultString(json, "[]"));
            if (!node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            });
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
