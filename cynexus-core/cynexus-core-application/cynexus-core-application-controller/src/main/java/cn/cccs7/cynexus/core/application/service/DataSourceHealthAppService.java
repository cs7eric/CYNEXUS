package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.DataSourceHealthDTO;
import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckItemBO;
import cn.cccs7.cynexus.core.domain.entity.DataSourceHealthCheckRunBO;
import cn.cccs7.cynexus.core.domain.repository.DataSourceHealthRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class DataSourceHealthAppService {

    private final DataJobExecutorClient dataJobExecutorClient;
    private final DataSourceHealthRepository dataSourceHealthRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataSourceHealthAppService(DataJobExecutorClient dataJobExecutorClient,
                                      DataSourceHealthRepository dataSourceHealthRepository) {
        this.dataJobExecutorClient = dataJobExecutorClient;
        this.dataSourceHealthRepository = dataSourceHealthRepository;
    }

    public DataSourceHealthDTO runSnapshot(boolean includeSlow) {
        Map<String, Object> payload = dataJobExecutorClient.dataSourceHealth(includeSlow);
        String runId = "ds_health_" + UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime generatedAt = asOffsetDateTime(payload.get("generated_at"), OffsetDateTime.now());
        Map<String, Object> summary = asMap(payload.get("summary"));
        Map<String, Double> previousSuccessRates = dataSourceHealthRepository.successRate24hBySource();

        DataSourceHealthCheckRunBO run = DataSourceHealthCheckRunBO.builder()
                .runId(runId)
                .triggerType("manual")
                .status(runStatus(summary))
                .startedAt(generatedAt)
                .finishedAt(OffsetDateTime.now())
                .durationMs(asInteger(payload.get("duration_ms")))
                .includeSlow(asBoolean(payload.get("include_slow")))
                .totalCount(asInteger(summary.get("total"), 0))
                .healthyCount(asInteger(summary.get("healthy"), 0))
                .degradedCount(asInteger(summary.get("degraded"), 0))
                .failedCount(asInteger(summary.get("failed"), 0))
                .skippedCount(asInteger(summary.get("skipped"), 0))
                .successRate(asBigDecimal(summary.get("success_rate")))
                .avgLatencyMs(asBigDecimal(summary.get("avg_latency_ms")))
                .rawPayload(toJson(payload))
                .extra("{}")
                .build();
        dataSourceHealthRepository.createRun(run);

        List<DataSourceHealthCheckItemBO> itemBos = new ArrayList<>();
        for (Object rawItem : asList(payload.get("items"))) {
            Map<String, Object> item = asMap(rawItem);
            String sourceCode = asString(item.get("source_code"));
            BigDecimal successRate24h = previousSuccessRates.containsKey(sourceCode)
                    ? BigDecimal.valueOf(previousSuccessRates.get(sourceCode))
                    : null;
            DataSourceHealthCheckItemBO itemBO = DataSourceHealthCheckItemBO.builder()
                    .runId(runId)
                    .sourceCode(sourceCode)
                    .sourceName(asString(item.get("source_name")))
                    .sourceType(asString(item.get("source_type")))
                    .category(asString(item.get("category")))
                    .provider(asString(item.get("provider")))
                    .endpoint(asString(item.get("endpoint")))
                    .status(asString(item.get("status")))
                    .connectivity(asBoolean(item.get("connectivity")))
                    .authenticityStatus(asString(item.get("authenticity_status")))
                    .latencyMs(asInteger(item.get("latency_ms")))
                    .sampleCount(asInteger(item.get("sample_count")))
                    .successRate24h(successRate24h)
                    .checkedAt(asOffsetDateTime(item.get("checked_at"), generatedAt))
                    .errorMessage(asString(item.get("error_message")))
                    .checks(toJson(item.get("checks")))
                    .rawPayload(toJson(item.get("raw_payload")))
                    .extra("{}")
                    .build();
            dataSourceHealthRepository.createItem(itemBO);
            itemBos.add(itemBO);
        }

        Map<String, Double> currentSuccessRates = dataSourceHealthRepository.successRate24hBySource();
        return toDto(payload, run, itemBos, currentSuccessRates);
    }

    public DataSourceHealthDTO latestSnapshot() {
        DataSourceHealthCheckRunBO run = dataSourceHealthRepository.latestRun();
        if (run == null) {
            return runSnapshot(false);
        }
        List<DataSourceHealthCheckItemBO> items = dataSourceHealthRepository.listItemsByRunId(run.getRunId());
        return toDto(Map.of(), run, items, dataSourceHealthRepository.successRate24hBySource());
    }

    private DataSourceHealthDTO toDto(Map<String, Object> payload,
                                      DataSourceHealthCheckRunBO run,
                                      List<DataSourceHealthCheckItemBO> items,
                                      Map<String, Double> successRates) {
        List<DataSourceHealthDTO.Item> dtoItems = items.stream()
                .map(item -> DataSourceHealthDTO.Item.builder()
                        .sourceCode(item.getSourceCode())
                        .sourceName(item.getSourceName())
                        .sourceType(item.getSourceType())
                        .category(item.getCategory())
                        .provider(item.getProvider())
                        .endpoint(item.getEndpoint())
                        .status(item.getStatus())
                        .connectivity(item.getConnectivity())
                        .authenticityStatus(item.getAuthenticityStatus())
                        .latencyMs(item.getLatencyMs())
                        .sampleCount(item.getSampleCount())
                        .successRate24h(successRates.containsKey(item.getSourceCode())
                                ? BigDecimal.valueOf(successRates.get(item.getSourceCode()))
                                : item.getSuccessRate24h())
                        .checkedAt(item.getCheckedAt())
                        .errorMessage(item.getErrorMessage())
                        .checks(item.getChecks())
                        .rawPayload(item.getRawPayload())
                        .build())
                .toList();
        return DataSourceHealthDTO.builder()
                .ok(run.getFailedCount() == null || run.getFailedCount() == 0)
                .generatedAt(asOffsetDateTime(payload.get("generated_at"), run.getFinishedAt()))
                .durationMs(run.getDurationMs())
                .includeSlow(run.getIncludeSlow())
                .summary(DataSourceHealthDTO.Summary.builder()
                        .total(run.getTotalCount())
                        .healthy(run.getHealthyCount())
                        .degraded(run.getDegradedCount())
                        .failed(run.getFailedCount())
                        .skipped(run.getSkippedCount())
                        .successRate(run.getSuccessRate())
                        .avgLatencyMs(run.getAvgLatencyMs())
                        .build())
                .run(DataSourceHealthDTO.Run.builder()
                        .runId(run.getRunId())
                        .triggerType(run.getTriggerType())
                        .status(run.getStatus())
                        .startedAt(run.getStartedAt())
                        .finishedAt(run.getFinishedAt())
                        .durationMs(run.getDurationMs())
                        .build())
                .items(dtoItems)
                .build();
    }

    private String runStatus(Map<String, Object> summary) {
        int failed = asInteger(summary.get("failed"), 0);
        int healthy = asInteger(summary.get("healthy"), 0);
        if (failed == 0) {
            return "success";
        }
        return healthy > 0 ? "partial" : "failed";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Integer asInteger(Object value) {
        return asInteger(value, null);
    }

    private static Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static OffsetDateTime asOffsetDateTime(Object value, OffsetDateTime defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return OffsetDateTime.parse(String.valueOf(value));
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of();
    }
}
