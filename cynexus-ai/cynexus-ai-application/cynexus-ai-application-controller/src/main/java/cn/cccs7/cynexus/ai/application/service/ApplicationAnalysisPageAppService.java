package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisPageDTO;
import cn.cccs7.cynexus.ai.api.request.ApplicationAnalysisPageRequest;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationAnalysisPageAppService {

    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";
    private static final String JSON_NULL = "null";
    private static final String UNSUPPORTED_EXECUTOR_MESSAGE =
            "Java application-analysis executor is not connected yet; existing persisted results are query-only.";

    private final ApplicationAnalysisPageRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public ApplicationAnalysisPageAppService(ApplicationAnalysisPageRepository repository) {
        this.repository = repository;
    }

    public ApplicationAnalysisPageDTO.TargetsResponse getTargets() {
        ApplicationAnalysisPageBO.Config config = repository.getDefaultConfig();
        List<ApplicationAnalysisPageDTO.Target> targets = repository.listTargets().stream()
                .map(this::toTargetDTO)
                .toList();
        ApplicationAnalysisPageDTO.ConfigPayload configPayload = ApplicationAnalysisPageDTO.ConfigPayload.builder()
                .version(1)
                .updatedAt(config == null ? null : config.getUpdatedAt())
                .horizon(toHorizonDTO(config))
                .items(targets)
                .build();
        return ApplicationAnalysisPageDTO.TargetsResponse.builder()
                .items(targets)
                .config(configPayload)
                .build();
    }

    @Transactional
    public ApplicationAnalysisPageDTO.TargetsResponse saveTargets(ApplicationAnalysisPageRequest.SaveTargetsRequest request) {
        ApplicationAnalysisPageRequest.Horizon horizon = request == null ? null : request.getHorizon();
        List<ApplicationAnalysisPageBO.Target> targets = request == null || request.getItems() == null
                ? List.of()
                : request.getItems().stream()
                        .map(this::toTargetBO)
                        .toList();
        repository.saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand.builder()
                .horizonDays(horizon == null ? null : horizon.getDays())
                .horizonSegments(horizon == null ? null : horizon.getSegments())
                .monthlyKeep(horizon == null ? null : horizon.getMonthlyKeep())
                .weeklyKeep(horizon == null ? null : horizon.getWeeklyKeep())
                .targets(targets)
                .build());
        return getTargets();
    }

    public ApplicationAnalysisPageDTO.AnalysisResultResponse getResult(String targetId) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            return null;
        }
        ApplicationAnalysisPageBO.CurrentResult current = repository.findCurrentResult(targetKey);
        if (current == null) {
            return null;
        }
        JsonNode root = parseJson(current.getPayloadJson());
        return ApplicationAnalysisPageDTO.AnalysisResultResponse.builder()
                .analysisInputJson(childRaw(root, "analysis_input", EMPTY_OBJECT))
                .analysisResultJson(childRaw(root, "analysis_result", EMPTY_OBJECT))
                .rawResultJson(childRaw(root, "raw_result", EMPTY_OBJECT))
                .metaResultPath("postgres://cynexus_appl_ai.application_analysis_result_current/" + targetKey)
                .metaHistory(toHistoryFiles(targetKey))
                .build();
    }

    public ApplicationAnalysisPageDTO.ActionResponse trigger(String targetId) {
        return ApplicationAnalysisPageDTO.ActionResponse.builder()
                .ok(false)
                .targetId(normalizeTargetKey(targetId))
                .error(UNSUPPORTED_EXECUTOR_MESSAGE)
                .items(List.of())
                .build();
    }

    public ApplicationAnalysisPageDTO.SchedulerStatus getSchedulerStatus() {
        List<ApplicationAnalysisPageBO.Target> targets = repository.listTargets();
        long enabled = targets.stream().filter(target -> Boolean.TRUE.equals(target.getEnabled())).count();
        long currentCount = targets.stream()
                .filter(target -> repository.findCurrentResult(target.getTargetKey()) != null)
                .count();
        return ApplicationAnalysisPageDTO.SchedulerStatus.builder()
                .running(false)
                .startedAt(null)
                .tickCount(0)
                .runsCount((int) currentCount)
                .enabledTargetCount((int) enabled)
                .totalTargetCount(targets.size())
                .inflight(EMPTY_OBJECT)
                .lastRun(EMPTY_OBJECT)
                .build();
    }

    public ApplicationAnalysisPageDTO.SchedulerControlResponse controlScheduler(String action) {
        return ApplicationAnalysisPageDTO.SchedulerControlResponse.builder()
                .ok(false)
                .status(getSchedulerStatus())
                .error("Java scheduler control for application-analysis is not connected yet: " + action)
                .build();
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse refreshRecent30(ApplicationAnalysisPageRequest.Recent30RefreshRequest request) {
        String targetId = request == null ? "" : normalizeTargetKey(request.getTargetId());
        return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder()
                .ok(false)
                .targetId(targetId)
                .error(UNSUPPORTED_EXECUTOR_MESSAGE)
                .snapshots(List.of())
                .build();
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse listRecent30(String targetId, int limit) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder()
                    .ok(false)
                    .targetId(targetKey)
                    .error("target " + targetKey + " not found")
                    .snapshots(List.of())
                    .build();
        }
        List<ApplicationAnalysisPageBO.DailySnapshot> snapshots = repository.listDailySnapshots(targetKey, limit);
        return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder()
                .ok(true)
                .targetId(targetKey)
                .snapshots(snapshots.stream().map(snapshot -> toDailySnapshotFile(targetKey, snapshot)).toList())
                .build();
    }

    public ApplicationAnalysisPageDTO.Recent30FullResponse listRecent30Full(String targetId, int limit) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            return ApplicationAnalysisPageDTO.Recent30FullResponse.builder()
                    .ok(false)
                    .targetId(targetKey)
                    .items(List.of())
                    .error("target " + targetKey + " not found")
                    .build();
        }
        List<ApplicationAnalysisPageDTO.Recent30FullItem> items = repository.listDailySnapshots(targetKey, limit)
                .stream()
                .map(snapshot -> ApplicationAnalysisPageDTO.Recent30FullItem.builder()
                        .filename(dailyFilename(snapshot))
                        .path(dailyPath(targetKey, snapshot))
                        .date(snapshot.getTradeDate().toString())
                        .sizeBytes(jsonSize(snapshot.getPayloadJson()))
                        .updatedAt(snapshot.getUpdatedAt())
                        .snapshot(toDailySnapshotDetail(target, snapshot))
                        .build())
                .toList();
        return ApplicationAnalysisPageDTO.Recent30FullResponse.builder()
                .ok(true)
                .targetId(targetKey)
                .items(items)
                .build();
    }

    public ApplicationAnalysisPageDTO.DailySnapshotResponse readRecent30(String targetId, LocalDate tradeDate) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        ApplicationAnalysisPageBO.DailySnapshot snapshot = repository.findDailySnapshot(targetKey, tradeDate);
        if (target == null || snapshot == null) {
            return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder()
                    .ok(false)
                    .targetId(targetKey)
                    .date(tradeDate == null ? null : tradeDate.toString())
                    .error("snapshot not found")
                    .snapshots(List.of())
                    .build();
        }
        JsonNode root = parseJson(snapshot.getPayloadJson());
        return ApplicationAnalysisPageDTO.DailySnapshotResponse.builder()
                .ok(true)
                .targetId(targetKey)
                .date(snapshot.getTradeDate().toString())
                .snapshotPath(dailyPath(targetKey, snapshot))
                .shortTermTrendJson(childRaw(root, "short_term_trend", JSON_NULL))
                .currentSituationJson(childRaw(root, "current_situation", JSON_NULL))
                .snapshot(toDailySnapshotDetail(target, snapshot))
                .snapshots(List.of())
                .build();
    }

    public String unsupportedExecutorMessage() {
        return UNSUPPORTED_EXECUTOR_MESSAGE;
    }

    private List<ApplicationAnalysisPageDTO.ResultFile> toHistoryFiles(String targetKey) {
        return repository.listHistory(targetKey, 20).stream()
                .map(row -> ApplicationAnalysisPageDTO.ResultFile.builder()
                        .filename(row.getHistoryKey())
                        .path("postgres://cynexus_appl_ai.application_analysis_result_history/" + targetKey + "/" + row.getHistoryKey())
                        .sizeBytes(jsonSize(row.getPayloadJson()))
                        .updatedAt(row.getAnalysisRunAt() == null ? row.getUpdatedAt() : row.getAnalysisRunAt())
                        .build())
                .toList();
    }

    private ApplicationAnalysisPageDTO.Target toTargetDTO(ApplicationAnalysisPageBO.Target target) {
        String targetKey = defaultString(target.getTargetKey(), buildTargetKey(target.getTargetType(), target.getSymbol()));
        return ApplicationAnalysisPageDTO.Target.builder()
                .id(targetKey)
                .targetType(defaultString(target.getTargetType(), "stock"))
                .symbol(defaultString(target.getSymbol(), ""))
                .market(target.getMarket())
                .name(defaultString(target.getName(), target.getSymbol()))
                .adjust(defaultString(target.getAdjust(), "qfq"))
                .enabled(target.getEnabled())
                .intervalMinutes(target.getIntervalMinutes())
                .tags(readStringList(target.getTagsJson()))
                .lastUpdatedAt(target.getUpdatedAt())
                .lastResultPath(repository.findCurrentResult(targetKey) == null
                        ? null
                        : "postgres://cynexus_appl_ai.application_analysis_result_current/" + targetKey)
                .build();
    }

    private ApplicationAnalysisPageBO.Target toTargetBO(ApplicationAnalysisPageRequest.Target target) {
        String targetType = defaultString(target.getTargetType(), "stock");
        String symbol = defaultString(target.getSymbol(), "").toUpperCase(Locale.ROOT);
        return ApplicationAnalysisPageBO.Target.builder()
                .targetKey(buildTargetKey(targetType, symbol))
                .legacyKey(defaultString(target.getId(), buildTargetKey(targetType, symbol)))
                .targetType(targetType)
                .symbol(symbol)
                .market(target.getMarket())
                .name(defaultString(target.getName(), symbol))
                .adjust(defaultString(target.getAdjust(), "qfq"))
                .enabled(target.getEnabled() == null || target.getEnabled())
                .intervalMinutes(target.getIntervalMinutes())
                .tagsJson(writeJson(target.getTags() == null ? List.of() : target.getTags(), EMPTY_ARRAY))
                .build();
    }

    private ApplicationAnalysisPageDTO.Horizon toHorizonDTO(ApplicationAnalysisPageBO.Config config) {
        if (config == null) {
            return ApplicationAnalysisPageDTO.Horizon.builder()
                    .days(120)
                    .segments(4)
                    .monthlyKeep(6)
                    .weeklyKeep(12)
                    .build();
        }
        return ApplicationAnalysisPageDTO.Horizon.builder()
                .days(config.getHorizonDays())
                .segments(config.getHorizonSegments())
                .monthlyKeep(config.getMonthlyKeep())
                .weeklyKeep(config.getWeeklyKeep())
                .build();
    }

    private ApplicationAnalysisPageDTO.DailySnapshotFile toDailySnapshotFile(
            String targetKey,
            ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        return ApplicationAnalysisPageDTO.DailySnapshotFile.builder()
                .filename(dailyFilename(snapshot))
                .path(dailyPath(targetKey, snapshot))
                .date(snapshot.getTradeDate().toString())
                .sizeBytes(jsonSize(snapshot.getPayloadJson()))
                .updatedAt(snapshot.getUpdatedAt())
                .build();
    }

    private ApplicationAnalysisPageDTO.DailySnapshotDetail toDailySnapshotDetail(
            ApplicationAnalysisPageBO.Target target,
            ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        JsonNode root = parseJson(snapshot.getPayloadJson());
        return ApplicationAnalysisPageDTO.DailySnapshotDetail.builder()
                .target(toTargetDTO(target))
                .date(snapshot.getTradeDate().toString())
                .updatedAt(snapshot.getUpdatedAt())
                .shortTermTrendJson(childRaw(root, "short_term_trend", JSON_NULL))
                .currentSituationJson(childRaw(root, "current_situation", JSON_NULL))
                .summaryJson(childRaw(root, "summary", JSON_NULL))
                .build();
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(defaultString(json, EMPTY_OBJECT));
        } catch (JsonProcessingException ex) {
            try {
                return objectMapper.readTree(EMPTY_OBJECT);
            } catch (JsonProcessingException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private String childRaw(JsonNode node, String fieldName, String fallbackRaw) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isMissingNode()) {
            return fallbackRaw;
        }
        return node.get(fieldName).toString();
    }

    private List<String> readStringList(String json) {
        JsonNode root = parseJson(defaultString(json, EMPTY_ARRAY));
        if (!root.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        root.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    private String writeJson(Object value, String fallback) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }

    private String dailyFilename(ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        return snapshot.getTradeDate() + ".json";
    }

    private String dailyPath(String targetKey, ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        return "postgres://cynexus_appl_ai.application_analysis_daily_snapshots/"
                + targetKey + "/" + dailyFilename(snapshot);
    }

    private Integer jsonSize(String json) {
        return defaultString(json, EMPTY_OBJECT).getBytes(StandardCharsets.UTF_8).length;
    }

    private String buildTargetKey(String targetType, String symbol) {
        return normalizeTargetKey(defaultString(targetType, "stock") + "-" + defaultString(symbol, ""));
    }

    private String normalizeTargetKey(String value) {
        return defaultString(value, "").toLowerCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
