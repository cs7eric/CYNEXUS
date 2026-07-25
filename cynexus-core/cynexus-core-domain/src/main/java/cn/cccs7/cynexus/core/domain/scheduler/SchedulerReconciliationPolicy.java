package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

/** Pure reconciliation rules for scheduler data-job status/history. */
public final class SchedulerReconciliationPolicy {

    private SchedulerReconciliationPolicy() {
    }

    public static boolean shouldReconcileDataJobStatus(SchedulerJobStatusBO status) {
        if (status == null || status.getDeletedAt() != null) {
            return false;
        }
        String lastStatus = status.getLastRunStatus();
        return Boolean.TRUE.equals(status.getRunning())
                || "running".equalsIgnoreCase(lastStatus)
                || "processing".equalsIgnoreCase(lastStatus);
    }

    public static OffsetDateTime safeStartedAt(OffsetDateTime preferred,
                                               OffsetDateTime fallback,
                                               OffsetDateTime endedAt,
                                               OffsetDateTime now) {
        OffsetDateTime startedAt = preferred != null ? preferred
                : (fallback != null ? fallback : (endedAt != null ? endedAt : now));
        if (endedAt != null && startedAt != null && startedAt.isAfter(endedAt)) {
            return endedAt;
        }
        return startedAt;
    }

    public static boolean isOpenHistoryStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return "running".equals(normalized)
                || "processing".equals(normalized)
                || "queued".equals(normalized)
                || "pending".equals(normalized);
    }

    public static String extractRunId(Map<String, Object> progress, Map<String, Object> extra) {
        Object fromProgress = readPayloadValue(progress, "lastDataJobRunId");
        if (fromProgress != null) {
            return String.valueOf(fromProgress);
        }
        Object fromExtra = firstPresent(extra, Set.of("lastDataJobRunId", "runId"));
        return fromExtra == null ? null : String.valueOf(fromExtra);
    }

    public static String extractRunIdFromSummary(Map<String, Object> summary) {
        Object fromPayload = readPayloadValue(summary, "lastDataJobRunId");
        return fromPayload == null ? null : String.valueOf(fromPayload);
    }

    public static DataJobTriggerResultBO dataJobRunSnapshot(String runId, Map<String, Object> run) {
        Map<String, Object> safeRun = run == null ? Map.of() : run;
        return DataJobTriggerResultBO.builder()
                .accepted(true)
                .runId(runId)
                .jobKey(asString(firstPresent(safeRun, Set.of("job_key", "jobKey"))))
                .status(asString(safeRun.get("status")))
                .result(safeRun)
                .build();
    }

    private static Object readPayloadValue(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || key == null) {
            return null;
        }
        if (source.containsKey(key)) {
            return source.get(key);
        }
        Object payload = source.get("payload");
        if (payload instanceof Map<?, ?> map && map.containsKey(key)) {
            return map.get(key);
        }
        return null;
    }

    private static Object firstPresent(Map<String, Object> source, Set<String> keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                return source.get(key);
            }
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}