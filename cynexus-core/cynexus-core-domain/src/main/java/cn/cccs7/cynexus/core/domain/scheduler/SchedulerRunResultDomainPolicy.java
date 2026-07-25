package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the domain facts persisted for a scheduler execution. */
public final class SchedulerRunResultDomainPolicy {

    private static final ZoneId CHINA_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    public RunFacts build(SchedulerJobStatusBO jobStatus, DataJobTriggerResultBO result,
                          boolean ok, String status, String error, OffsetDateTime updatedAt) {
        Map<String, Object> extra = buildRunExtra(jobStatus, result, ok, status, error);
        return new RunFacts(extra,
                buildProgressEnvelope(extra, status, updatedAt),
                buildResultSummary(result, extra, status, error, updatedAt),
                extractTargetsProcessed(result, extra),
                buildRunRemark(result, ok, status, error));
    }

    public Map<String, Object> progress(Map<String, Object> extra, String status, OffsetDateTime updatedAt) {
        return buildProgressEnvelope(extra == null ? Map.of() : extra, status, updatedAt);
    }

    public Map<String, Object> resultSummary(DataJobTriggerResultBO result, Map<String, Object> extra,
                                             String status, String error, OffsetDateTime updatedAt) {
        return buildResultSummary(result, extra == null ? Map.of() : extra, status, error, updatedAt);
    }

    public Map<String, Object> metadata(Map<String, Object> result) {
        return extractMetadata(result);
    }

    public BigDecimal durationSeconds(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            return null;
        }
        return BigDecimal.valueOf(java.time.Duration.between(startedAt, endedAt).toMillis())
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildRunExtra(SchedulerJobStatusBO jobStatus, DataJobTriggerResultBO result,
                                               boolean ok, String status, String error) {
        Map<String, Object> extra = new LinkedHashMap<>(readExtra(jobStatus == null ? null : jobStatus.getExtra()));
        Map<String, Object> resultMap = result == null || result.getResult() == null ? Map.of() : result.getResult();
        Map<String, Object> metadata = extractMetadata(resultMap);
        extra.put("lastDataJobRunId", result == null ? null : result.getRunId());
        extra.put("lastDataJobKey", result == null ? null : result.getJobKey());
        extra.put("lastStatus", status);
        extra.put("lastRunOk", ok);
        extra.put("lastRunError", error);
        extra.put("rowsRead", firstNumber(resultMap, metadata, "rows_read", "rowsRead"));
        extra.put("rowsWritten", firstNumber(resultMap, metadata, "rows_written", "rowsWritten"));
        Object pipeline = firstPresent(metadata, Set.of("pipeline"));
        if (pipeline != null) extra.put("pipeline", pipeline);
        copyIfPresent(metadata, extra, "lastStep");
        Object stepProgress = withTotalCodeProgress(firstPresent(metadata, Set.of("currentStepProgress")), metadata);
        if (stepProgress != null) extra.put("currentStepProgress", stepProgress);
        copyIfPresent(metadata, extra, "qcSummary");
        copyIfPresent(metadata, extra, "lastCompletedSteps");
        copyIfPresent(metadata, extra, "lastStepResults");
        Object plannedSteps = firstNumber(resultMap, metadata, "planned_steps", "plannedSteps");
        if (plannedSteps != null) extra.put("plannedSteps", plannedSteps);
        Object message = firstPresent(metadata, Set.of("lastMessage", "message"));
        if (message == null) message = extra.get("rowsWritten") == null ? status : "rows_written=" + extra.get("rowsWritten");
        extra.put("lastMessage", message);
        return extra;
    }

    private Integer extractTargetsProcessed(DataJobTriggerResultBO result, Map<String, Object> extra) {
        Object completed = extra.get("lastCompletedSteps");
        if (completed instanceof List<?> list && !list.isEmpty()) return list.size();
        Integer rowsWritten = asInteger(extra.get("rowsWritten"));
        if (rowsWritten != null) return rowsWritten;
        if (result != null && result.getResult() != null && result.getResult().get("items") instanceof List<?> list) return list.size();
        return null;
    }

    private String buildRunRemark(DataJobTriggerResultBO result, boolean ok, String status, String error) {
        if (!ok) return error == null || error.isBlank() ? "failed: " + status : error;
        if (result == null) return CoreConstants.STATUS_SUCCESS;
        Map<String, Object> resultMap = result.getResult() == null ? Map.of() : result.getResult();
        Map<String, Object> metadata = extractMetadata(resultMap);
        StringBuilder builder = new StringBuilder("run_id=").append(result.getRunId())
                .append(", data_job=").append(result.getJobKey());
        Object rowsRead = firstNumber(resultMap, metadata, "rows_read", "rowsRead");
        Object rowsWritten = firstNumber(resultMap, metadata, "rows_written", "rowsWritten");
        Object completed = firstPresent(metadata, Set.of("lastCompletedSteps"));
        Object message = firstPresent(metadata, Set.of("lastMessage", "message"));
        if (rowsRead != null) builder.append(", rows_read=").append(rowsRead);
        if (rowsWritten != null) builder.append(", rows_written=").append(rowsWritten);
        if (completed instanceof List<?> list && !list.isEmpty()) builder.append(", completed_steps=").append(list.size());
        if (message != null) builder.append(", ").append(message);
        return builder.toString();
    }

    private Map<String, Object> buildProgressEnvelope(Map<String, Object> extra, String status, OffsetDateTime updatedAt) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        putIfPresent(metrics, "rowsRead", extra.get("rowsRead"));
        putIfPresent(metrics, "rowsWritten", extra.get("rowsWritten"));
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "lastDataJobRunId", extra.get("lastDataJobRunId"));
        putIfPresent(payload, "lastDataJobKey", extra.get("lastDataJobKey"));
        putIfPresent(payload, "pipeline", extra.get("pipeline"));
        putIfPresent(payload, "qcSummary", extra.get("qcSummary"));
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("schemaVersion", 1);
        progress.put("type", inferProgressType(extra));
        progress.put("status", status);
        progress.put("percent", inferPercent(extra, status));
        progress.put("currentStep", firstPresent(extra, Set.of("lastStep", "currentStep")));
        progress.put("currentStepProgress", firstPresent(extra, Set.of("currentStepProgress", "stepProgress")));
        progress.put("completedSteps", firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps")));
        progress.put("stepResults", firstPresent(extra, Set.of("lastStepResults", "stepResults")));
        progress.put("totalSteps", firstPresent(extra, Set.of("plannedSteps", "planned_steps", "totalSteps")));
        progress.put("message", firstPresent(extra, Set.of("lastMessage", "message")));
        progress.put("metrics", metrics);
        progress.put("payload", payload);
        progress.put("updatedAt", updatedAt == null ? OffsetDateTime.now(CHINA_ZONE).toString() : updatedAt.toString());
        return progress;
    }

    private Map<String, Object> buildResultSummary(DataJobTriggerResultBO result, Map<String, Object> extra,
                                                    String status, String error, OffsetDateTime updatedAt) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        putIfPresent(metrics, "rowsRead", extra.get("rowsRead"));
        putIfPresent(metrics, "rowsWritten", extra.get("rowsWritten"));
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "lastDataJobRunId", extra.get("lastDataJobRunId"));
        putIfPresent(payload, "lastDataJobKey", extra.get("lastDataJobKey"));
        putIfPresent(payload, "pipeline", extra.get("pipeline"));
        putIfPresent(payload, "completedSteps", firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps")));
        putIfPresent(payload, "stepResults", firstPresent(extra, Set.of("lastStepResults", "stepResults")));
        putIfPresent(payload, "qcSummary", extra.get("qcSummary"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("type", inferProgressType(extra));
        summary.put("status", status);
        summary.put("message", error == null || error.isBlank() ? firstPresent(extra, Set.of("lastMessage", "message")) : error);
        summary.put("completedCount", completedCount(extra));
        summary.put("failedCount", failedCount(extra, result, status, error));
        summary.put("metrics", metrics);
        summary.put("payload", payload);
        summary.put("updatedAt", updatedAt == null ? OffsetDateTime.now(CHINA_ZONE).toString() : updatedAt.toString());
        return summary;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) { if (value != null) target.put(key, value); }
    private static Object withTotalCodeProgress(Object value, Map<String, Object> metadata) {
        if (!(value instanceof Map<?, ?> map)) {
            return value;
        }
        Map<String, Object> progress = new LinkedHashMap<>();
        map.forEach((key, item) -> progress.put(String.valueOf(key), item));
        putIfAbsent(progress, "completedCodes", firstPresent(metadata, Set.of("completed_codes", "completedCodes")));
        putIfAbsent(progress, "failedCodes", firstPresent(metadata, Set.of("failed_codes", "failedCodes")));
        putIfAbsent(progress, "pendingCodes", firstPresent(metadata, Set.of("pending_codes", "pendingCodes")));
        putIfAbsent(progress, "totalCodes", firstPresent(metadata, Set.of("total_codes", "totalCodes")));
        return progress;
    }
    private static void putIfAbsent(Map<String, Object> target, String key, Object value) {
        if (value != null && !target.containsKey(key)) target.put(key, value);
    }
    private static String inferProgressType(Map<String, Object> extra) {
        return extra.containsKey("lastStepResults") || extra.containsKey("lastCompletedSteps") ? "chain" : "job";
    }
    private static BigDecimal inferPercent(Map<String, Object> extra, String status) {
        if (CoreConstants.STATUS_SUCCESS.equalsIgnoreCase(status) || CoreConstants.STATUS_SKIPPED.equalsIgnoreCase(status)) return BigDecimal.valueOf(100);
        if (CoreConstants.STATUS_FAILED.equalsIgnoreCase(status)) return null;
        Integer planned = asInteger(firstPresent(extra, Set.of("plannedSteps", "planned_steps", "totalSteps")));
        Object completed = firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps"));
        Object steps = firstPresent(extra, Set.of("lastStepResults", "stepResults"));
        int denominator = planned != null && planned > 0 ? planned : steps instanceof List<?> list && !list.isEmpty() ? list.size() : 0;
        return completed instanceof List<?> list && denominator > 0
                ? BigDecimal.valueOf(list.size() * 100L).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP) : null;
    }
    private static Integer completedCount(Map<String, Object> extra) {
        Object value = firstPresent(extra, Set.of("lastCompletedSteps", "completedSteps"));
        return value instanceof List<?> list ? list.size() : null;
    }
    private static Integer failedCount(Map<String, Object> extra, DataJobTriggerResultBO result, String status, String error) {
        Object value = firstPresent(extra, Set.of("lastStepResults", "stepResults"));
        if (value instanceof List<?> list) {
            int count = 0;
            for (Object item : list) if (item instanceof Map<?, ?> map && Boolean.FALSE.equals(asBoolean(map.get("ok")))) count++;
            return count;
        }
        return CoreConstants.STATUS_FAILED.equalsIgnoreCase(status) || error != null && !error.isBlank() ? 1 : result == null ? null : 0;
    }
    private static Map<String, Object> extractMetadata(Map<String, Object> value) {
        Object metadata = value == null ? null : value.get("metadata");
        return metadata instanceof Map<?, ?> map ? asStringObjectMap(map) : Map.of();
    }
    private static Object firstNumber(Map<String, Object> first, Map<String, Object> second, String snake, String camel) {
        Object value = firstPresent(first, Set.of(snake, camel));
        return value == null ? firstPresent(second, Set.of(snake, camel)) : value;
    }
    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) target.put(key, source.get(key));
    }
    private static Map<String, Object> readExtra(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node != null && node.isObject() ? asStringObjectMap(new com.fasterxml.jackson.databind.ObjectMapper().convertValue(node, Map.class)) : new LinkedHashMap<>();
        } catch (Exception ignored) { return new LinkedHashMap<>(); }
    }
    private static Map<String, Object> asStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
    private static Object firstPresent(Map<?, ?> map, Set<String> keys) {
        if (map == null) return null;
        for (String key : keys) if (map.containsKey(key) && map.get(key) != null) return map.get(key);
        return null;
    }
    private static Integer asInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? null : Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; }
    }
    private static Boolean asBoolean(Object value) { return value instanceof Boolean bool ? bool : value == null ? null : Boolean.parseBoolean(String.valueOf(value)); }

    public record RunFacts(Map<String, Object> extra, Map<String, Object> progress, Map<String, Object> resultSummary,
                           Integer targetsProcessed, String remark) { }
}
