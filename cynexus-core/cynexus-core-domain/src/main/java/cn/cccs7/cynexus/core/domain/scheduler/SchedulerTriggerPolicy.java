package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerItem;
import cn.cccs7.cynexus.core.domain.valueobject.scheduler.SchedulerTriggerOutcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SchedulerTriggerPolicy {

    private static final String MARKET_SENTIMENT_CHAIN_JOB_CODE = "market_sentiment_chain_refresh";
    private static final Set<String> PROGRESS_POLL_JOB_CODES = Set.of(
            MARKET_SENTIMENT_CHAIN_JOB_CODE,
            "a_stock_fund_flow_history_bootstrap_120"
    );
    private static final int MARKET_SENTIMENT_CHAIN_STEP_COUNT = 17;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SchedulerTriggerPolicy() {
    }

    public static Optional<SchedulerDataJobKeyResolver.ResolvedDataJob> resolveDataJob(SchedulerJobBO job) {
        Map<String, Object> extra = readMap(job == null ? null : job.getExtra());
        String dataJobKey = asString(firstPresent(extra,
                Set.of("data_job_key", "dataJobKey", "job_key", "jobKey")));
        if (dataJobKey != null && !dataJobKey.isBlank()) {
            Map<String, Object> defaultParams = asMap(firstPresent(extra,
                    Set.of("default_params", "defaultParams")));
            return Optional.of(new SchedulerDataJobKeyResolver.ResolvedDataJob(dataJobKey, defaultParams));
        }
        return SchedulerDataJobKeyResolver.resolve(job == null ? null : job.getCode());
    }

    public static Map<String, Object> triggerParams(
            SchedulerDataJobKeyResolver.ResolvedDataJob dataJob, String targetDate) {
        Map<String, Object> params = new LinkedHashMap<>(dataJob.defaultParams());
        if (targetDate != null && !targetDate.isBlank()) {
            params.put("target_date", targetDate);
            params.putIfAbsent("trade_date", targetDate);
            params.putIfAbsent("snapshot_date", targetDate);
        }
        return params;
    }

    public static boolean requiresProgressPolling(String jobCode) {
        return PROGRESS_POLL_JOB_CODES.contains(jobCode);
    }

    public static Map<String, Object> startingExtra(SchedulerJobBO job, String dataJobKey, String currentExtra) {
        Map<String, Object> extra = new LinkedHashMap<>(readMap(currentExtra));
        extra.put("lastStep", "starting");
        if (job != null && MARKET_SENTIMENT_CHAIN_JOB_CODE.equals(job.getCode())) {
            extra.put("lastCompletedSteps", List.of());
            extra.put("lastStepResults", List.of());
            extra.put("plannedSteps", MARKET_SENTIMENT_CHAIN_STEP_COUNT);
        }
        String jobCode = job == null ? "scheduler job" : job.getCode();
        extra.put("lastMessage", jobCode + " 已在后台启动");
        extra.put("lastDataJobKey", dataJobKey);
        return extra;
    }

    public static int defaultProgressStepCount() {
        return MARKET_SENTIMENT_CHAIN_STEP_COUNT;
    }

    public static SchedulerTriggerOutcome completed(String jobCode, DataJobTriggerResultBO result) {
        List<SchedulerTriggerItem> items = triggerItems(result == null ? null : result.getResult());
        int failedCount = (int) items.stream().filter(item -> !Boolean.TRUE.equals(item.ok())).count();
        int count = items.isEmpty() && result != null && Boolean.TRUE.equals(result.getAccepted()) ? 1 : items.size();
        boolean ok = SchedulerManagementPolicy.isAcceptedSuccess(result);
        return new SchedulerTriggerOutcome(
                ok,
                jobCode,
                result == null ? false : result.getAccepted(),
                result == null ? null : result.getRunId(),
                SchedulerManagementPolicy.normalizeTriggerStatus(result),
                count,
                ok ? failedCount : Math.max(1, failedCount),
                items,
                SchedulerManagementPolicy.triggerError(result));
    }

    public static SchedulerTriggerOutcome failed(String jobCode, String error) {
        return new SchedulerTriggerOutcome(false, jobCode, false, null, CoreConstants.STATUS_FAILED, 0, 1, List.of(), error);
    }

    public static SchedulerTriggerOutcome processing(String jobCode) {
        SchedulerTriggerItem item = new SchedulerTriggerItem(
                jobCode, true, CoreConstants.STATUS_PROCESSING, null, "job 已在后台启动, 请刷新查看进度");
        return new SchedulerTriggerOutcome(true, jobCode, true, null, CoreConstants.STATUS_PROCESSING, 1, 0, List.of(item), null);
    }

    private static List<SchedulerTriggerItem> triggerItems(Map<String, Object> result) {
        Object rawItems = result == null ? null : result.get("items");
        if (!(rawItems instanceof List<?>)) {
            Object metadata = result == null ? null : result.get("metadata");
            if (metadata instanceof Map<?, ?> map) {
                rawItems = map.get("lastStepResults");
            }
        }
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }
        List<SchedulerTriggerItem> items = new ArrayList<>();
        for (Object raw : list) {
            if (raw instanceof Map<?, ?> map) {
                items.add(new SchedulerTriggerItem(
                        asString(map.get("jobId")),
                        asBoolean(map.get("ok")),
                        asString(map.get("lastStatus")),
                        asString(firstPresent(map, Set.of("lastRunError", "last_error"))),
                        asString(map.get("lastMessage"))));
            }
        }
        return List.copyOf(items);
    }

    private static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return asMap(OBJECT_MAPPER.readValue(json, Map.class));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static Object firstPresent(Map<?, ?> map, Set<String> keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean asBoolean(Object value) {
        return value instanceof Boolean bool ? bool : value == null ? null : Boolean.parseBoolean(String.valueOf(value));
    }
}
