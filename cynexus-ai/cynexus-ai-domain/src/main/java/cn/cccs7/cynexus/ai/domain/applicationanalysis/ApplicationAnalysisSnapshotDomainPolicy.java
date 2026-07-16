package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

/** Domain rules for sanitizing and merging recent-30 analysis snapshots. */
public final class ApplicationAnalysisSnapshotDomainPolicy {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public ObjectNode sanitize(JsonNode rawRoot, ApplicationAnalysisPageBO.Target target,
                               Map<String, Object> analysisInput) {
        ObjectNode root = rawRoot != null && rawRoot.isObject() ? (ObjectNode) rawRoot : mapper.createObjectNode();
        JsonNode source = root.get("analysis_result");
        ObjectNode analysis = source != null && source.isObject() ? (ObjectNode) source : mapper.createObjectNode();
        if (!(source != null && source.isObject())) {
            copyIfPresent(root, analysis, "target");
            copyIfPresent(root, analysis, "data_quality");
            copyIfPresent(root, analysis, "short_term_trend");
            copyIfPresent(root, analysis, "current_situation");
        }
        ObjectNode cleaned = mapper.createObjectNode();
        JsonNode targetNode = analysis.get("target");
        cleaned.set("target", targetNode != null && targetNode.isObject() ? targetNode : targetNode(target));
        cleaned.set("data_quality", normalizeDataQuality(analysis.get("data_quality"), analysisInput));
        cleaned.set("short_term_trend", analysis.path("short_term_trend").isObject() ? analysis.get("short_term_trend") : mapper.nullNode());
        cleaned.set("current_situation", analysis.path("current_situation").isObject() ? analysis.get("current_situation") : mapper.nullNode());
        addMissingWarnings(cleaned);
        ObjectNode result = mapper.createObjectNode();
        result.set("analysis_result", cleaned);
        return result;
    }

    public SnapshotFacts mergeSnapshot(ApplicationAnalysisPageBO.Target target, ObjectNode runPayload,
                                       ObjectNode existingPayload, LocalDate requestedDate, OffsetDateTime now) {
        LocalDate date = requestedDate == null ? latestTradeDate(runPayload) : requestedDate;
        String targetKey = normalizeTargetKey(target.getTargetKey());
        String path = dailyPath(targetKey, date);
        ObjectNode newAnalysis = copyObject(runPayload.path("analysis_result"));
        boolean hasNewData = isMeaningful(newAnalysis.get("short_term_trend"))
                || isMeaningful(newAnalysis.get("current_situation"))
                || isMeaningful(newAnalysis.get("summary"));
        if (!hasNewData) return new SnapshotFacts(date, path, false, "no_new_data", null);
        ObjectNode existing = existingPayload == null ? mapper.createObjectNode() : existingPayload;
        ObjectNode serialized = existing.deepCopy();
        serialized.set("target", targetNode(target));
        serialized.put("date", date.toString());
        serialized.put("updated_at", (now == null ? OffsetDateTime.now(SHANGHAI) : now).toString());
        serialized.set("recent_window", runPayload.path("analysis_input").path("recent_window"));
        setIfMeaningful(serialized, "short_term_trend", newAnalysis.get("short_term_trend"), existing);
        setIfMeaningful(serialized, "current_situation", newAnalysis.get("current_situation"), existing);
        setIfMeaningful(serialized, "summary", newAnalysis.get("summary"), existing);
        ObjectNode merged = deepMerge(copyObject(existing.path("analysis_result")), newAnalysis);
        mergeWarnings(merged, existing.path("analysis_result"), newAnalysis);
        serialized.set("analysis_result", merged);
        return new SnapshotFacts(date, path, true, null, serialized);
    }

    public ObjectNode readObject(String rawJson) {
        try {
            JsonNode value = mapper.readTree(rawJson == null || rawJson.isBlank() ? "{}" : rawJson);
            return value != null && value.isObject() ? (ObjectNode) value : mapper.createObjectNode();
        } catch (Exception ignored) {
            return mapper.createObjectNode();
        }
    }

    public LocalDate latestTradeDate(ObjectNode runPayload) {
        JsonNode items = runPayload.path("analysis_input").path("bars").path("daily").path("items");
        if (items.isArray() && !items.isEmpty()) {
            long timestamp = items.get(items.size() - 1).path("timestamp").asLong(0);
            if (timestamp > 0) return Instant.ofEpochMilli(timestamp).atZone(SHANGHAI).toLocalDate();
        }
        return LocalDate.now(SHANGHAI);
    }

    public String normalizeTargetKey(String value) {
        return defaultString(value, "").toLowerCase();
    }

    public String dailyPath(String targetKey, LocalDate date) {
        return "postgres://cynexus_appl_ai.application_analysis_daily_snapshots/" + targetKey + "/" + date + ".json";
    }

    public String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    public int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private ObjectNode normalizeDataQuality(JsonNode value, Map<String, Object> input) {
        ObjectNode quality = copyObject(value);
        if (!quality.has("daily_bars_count")) quality.put("daily_bars_count", dailyBarsCount(input));
        if (!quality.has("warnings") || !quality.get("warnings").isArray()) quality.set("warnings", mapper.createArrayNode());
        return quality;
    }
    private void addMissingWarnings(ObjectNode cleaned) {
        ArrayNode warnings = (ArrayNode) cleaned.path("data_quality").path("warnings");
        if (!cleaned.path("short_term_trend").isObject()) warnings.add("AI 未返回 short_term_trend");
        if (!cleaned.path("current_situation").isObject()) warnings.add("AI 未返回 current_situation");
    }
    private int dailyBarsCount(Map<String, Object> input) {
        try {
            Map<?, ?> bars = (Map<?, ?>) input.get("bars");
            Map<?, ?> daily = (Map<?, ?>) bars.get("daily");
            Object items = daily.get("items");
            return items instanceof java.util.List<?> list ? list.size() : 0;
        } catch (Exception ignored) { return 0; }
    }
    private ObjectNode targetNode(ApplicationAnalysisPageBO.Target target) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", target.getTargetKey());
        node.put("target_type", defaultString(target.getTargetType(), "stock"));
        node.put("symbol", defaultString(target.getSymbol(), ""));
        node.put("name", defaultString(target.getName(), target.getSymbol()));
        node.put("adjust", defaultString(target.getAdjust(), "qfq"));
        return node;
    }
    private ObjectNode deepMerge(ObjectNode base, ObjectNode overlay) {
        overlay.fields().forEachRemaining(entry -> {
            JsonNode oldValue = base.get(entry.getKey());
            JsonNode newValue = entry.getValue();
            base.set(entry.getKey(), oldValue != null && oldValue.isObject() && newValue.isObject()
                    ? deepMerge(copyObject(oldValue), copyObject(newValue)) : newValue);
        });
        return base;
    }
    private void mergeWarnings(ObjectNode merged, JsonNode existing, JsonNode newer) {
        ObjectNode quality = copyObject(merged.path("data_quality"));
        ArrayNode warnings = mapper.createArrayNode();
        addWarnings(warnings, existing.path("data_quality").path("warnings"));
        addWarnings(warnings, newer.path("data_quality").path("warnings"));
        quality.set("warnings", warnings);
        merged.set("data_quality", quality);
    }
    private void addWarnings(ArrayNode target, JsonNode source) {
        if (!source.isArray()) return;
        for (JsonNode item : source) {
            String warning = item.asText("");
            boolean exists = false;
            for (JsonNode current : target) if (warning.equals(current.asText())) exists = true;
            if (!warning.isBlank() && !exists) target.add(warning);
        }
    }
    private void setIfMeaningful(ObjectNode target, String field, JsonNode value, ObjectNode existing) {
        if (isMeaningful(value)) target.set(field, value);
        else if (existing.has(field)) target.set(field, existing.get(field));
    }
    private boolean isMeaningful(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) return false;
        return value.isObject() || value.isArray() ? !value.isEmpty() : !value.asText("").isBlank();
    }
    private void copyIfPresent(ObjectNode source, ObjectNode target, String field) { if (source.has(field)) target.set(field, source.get(field)); }
    private ObjectNode copyObject(JsonNode value) { return value != null && value.isObject() ? ((ObjectNode) value).deepCopy() : mapper.createObjectNode(); }
    private String defaultString(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    public record SnapshotFacts(LocalDate date, String path, boolean updated, String skipReason, ObjectNode payload) { }
}
