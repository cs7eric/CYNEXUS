package cn.cccs7.cynexus.ai.domain.applicationanalysis.impl;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisKlineInputPort;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainPolicy;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationAnalysisPageDomainServiceImpl implements ApplicationAnalysisPageDomainService {

    private static final String EMPTY_OBJECT = "{}";
    private static final String EMPTY_ARRAY = "[]";
    private static final String JSON_NULL = "null";

    private final ApplicationAnalysisPageRepository repository;
    private final ApplicationAnalysisExecutionDomainService executionService;
    private final ApplicationAnalysisKlineInputPort klineInputPort;
    private final ApplicationAnalysisPageDomainPolicy policy = new ApplicationAnalysisPageDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public ApplicationAnalysisPageDomainServiceImpl(ApplicationAnalysisPageRepository repository,
                                                    ApplicationAnalysisExecutionDomainService executionService,
                                                    ApplicationAnalysisKlineInputPort klineInputPort) {
        this.repository = repository;
        this.executionService = executionService;
        this.klineInputPort = klineInputPort;
    }

    @Override
    public TargetsView getTargets() {
        ApplicationAnalysisPageBO.Config config = repository.getDefaultConfig();
        return new TargetsView(config, repository.listTargets().stream().map(this::targetView).toList());
    }

    @Override
    public TargetsView saveTargets(SaveTargetsCommand command) {
        List<ApplicationAnalysisPageBO.Target> targets = command == null || command.targets() == null
                ? List.of() : command.targets().stream().map(this::toTarget).toList();
        repository.saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand.builder()
                .horizonDays(command == null ? null : command.horizonDays())
                .horizonSegments(command == null ? null : command.horizonSegments())
                .monthlyKeep(command == null ? null : command.monthlyKeep())
                .weeklyKeep(command == null ? null : command.weeklyKeep())
                .targets(targets).build());
        return getTargets();
    }

    @Override
    public ResultView getResult(String targetId) {
        String targetKey = policy.normalizeTargetKey(targetId);
        if (repository.findTarget(targetKey) == null) return null;
        ApplicationAnalysisPageBO.CurrentResult current = repository.findCurrentResult(targetKey);
        if (current == null) return null;
        JsonNode root = parseJson(current.getPayloadJson());
        return new ResultView(targetKey, childRaw(root, "analysis_input", EMPTY_OBJECT),
                childRaw(root, "analysis_result", EMPTY_OBJECT), childRaw(root, "raw_result", EMPTY_OBJECT),
                historyFiles(targetKey));
    }

    @Override
    public ActionView trigger(String targetId) {
        String targetKey = normalizeTargetKey(targetId);
        if (!targetKey.isBlank()) {
            ActionItem item = runActionItem(targetKey);
            return new ActionView(item.ok(), targetKey, item.error(), List.of(item));
        }
        List<ActionItem> items = repository.listTargets().stream()
                .filter(target -> Boolean.TRUE.equals(target.getEnabled()))
                .map(target -> runActionItem(target.getTargetKey())).toList();
        boolean ok = items.stream().allMatch(ActionItem::ok);
        return new ActionView(ok, null, ok ? null : "Some application-analysis targets failed", items);
    }

    @Override
    public SchedulerStatusView getSchedulerStatus() {
        List<ApplicationAnalysisPageBO.Target> targets = repository.listTargets();
        long enabled = targets.stream().filter(target -> Boolean.TRUE.equals(target.getEnabled())).count();
        long currentCount = targets.stream()
                .filter(target -> repository.findCurrentResult(target.getTargetKey()) != null).count();
        return new SchedulerStatusView(false, null, 0, (int) currentCount, (int) enabled,
                targets.size(), EMPTY_OBJECT, EMPTY_OBJECT);
    }

    @Override
    public ControlView controlScheduler(String action) {
        return new ControlView(false, getSchedulerStatus(),
                "Java scheduler control for application-analysis is not connected yet: " + action);
    }

    @Override
    public DailySnapshotView refreshRecent30(Recent30Command command) {
        String targetId = command == null ? "" : policy.normalizeTargetKey(command.targetId());
        LocalDate date = policy.parseDate(command == null ? null : command.date());
        ApplicationAnalysisPageBO.Target target = executionService.requireTarget(targetId);
        ApplicationAnalysisExecutionDomainService.ExecutionResult result = executionService.execute(
                new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                        target, date, klineInputPort.buildRecent30Input(target)));
        return dailySnapshotView(result);
    }

    @Override
    public ResultView run(RunCommand command) {
        String targetType = policy.defaultString(command == null ? null : command.targetType(), "stock");
        String symbol = policy.defaultString(command == null ? null : command.symbol(), "").toUpperCase(Locale.ROOT);
        if (symbol.isBlank()) {
            throw new cn.cccs7.cynexus.common.core.exception.BizException(
                    cn.cccs7.cynexus.common.core.exception.ErrorCode.BAD_REQUEST, "symbol is required");
        }
        String targetKey = policy.buildTargetKey(targetType, symbol);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            target = ApplicationAnalysisPageBO.Target.builder().targetKey(targetKey).legacyKey(targetKey)
                    .targetType(targetType).symbol(symbol)
                    .name(policy.defaultString(command == null ? null : command.name(), symbol))
                    .adjust(policy.defaultString(command == null ? null : command.adjust(), "qfq"))
                    .enabled(true).intervalMinutes(60).tagsJson(EMPTY_ARRAY).build();
            repository.saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand.builder()
                    .targets(List.of(target)).build());
        }
        executionService.execute(new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                target, null, klineInputPort.buildRecent30Input(target)));
        return getResult(targetKey);
    }

    @Override
    public DailySnapshotView listRecent30(String targetId, int limit) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) return new DailySnapshotView(false, "target " + targetKey + " not found", targetKey,
                null, null, null, null, null, null, List.of(), null);
        List<SnapshotFile> snapshots = repository.listDailySnapshots(targetKey, limit).stream()
                .map(snapshot -> snapshotFile(targetKey, snapshot)).toList();
        return new DailySnapshotView(true, null, targetKey, null, null, null, null, null, null, snapshots, null);
    }

    @Override
    public FullView listRecent30Full(String targetId, int limit) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) return new FullView(false, targetKey, List.of(), "target " + targetKey + " not found");
        List<FullItem> items = repository.listDailySnapshots(targetKey, limit).stream().map(snapshot ->
                new FullItem(dailyFilename(snapshot), dailyPath(targetKey, snapshot), snapshot.getTradeDate(),
                        jsonSize(snapshot.getPayloadJson()), snapshot.getUpdatedAt(), snapshotDetail(target, snapshot)))
                .toList();
        return new FullView(true, targetKey, items, null);
    }

    @Override
    public DailySnapshotView readRecent30(String targetId, LocalDate tradeDate) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        ApplicationAnalysisPageBO.DailySnapshot snapshot = repository.findDailySnapshot(targetKey, tradeDate);
        if (target == null || snapshot == null) return new DailySnapshotView(false, "snapshot not found", targetKey,
                tradeDate, null, null, null, null, null, List.of(), null);
        JsonNode root = parseJson(snapshot.getPayloadJson());
        return new DailySnapshotView(true, null, targetKey, snapshot.getTradeDate(), dailyPath(targetKey, snapshot),
                childRaw(root, "short_term_trend", JSON_NULL), childRaw(root, "current_situation", JSON_NULL),
                null, null, List.of(), snapshotDetail(target, snapshot));
    }

    private ActionItem runActionItem(String targetKey) {
        try {
            ApplicationAnalysisPageBO.Target target = executionService.requireTarget(targetKey);
            executionService.execute(new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                    target, null, klineInputPort.buildRecent30Input(target)));
            return new ActionItem(targetKey, true, null);
        } catch (Exception exception) {
            return new ActionItem(targetKey, false, exception.getMessage());
        }
    }

    private DailySnapshotView dailySnapshotView(ApplicationAnalysisExecutionDomainService.ExecutionResult result) {
        ApplicationAnalysisPageBO.Target target = repository.findTarget(result.targetKey());
        ApplicationAnalysisPageBO.DailySnapshot saved = repository.findDailySnapshot(result.targetKey(), result.date());
        JsonNode root = saved == null ? result.payload() : parseJson(saved.getPayloadJson());
        return new DailySnapshotView(result.ok(), null, result.targetKey(), result.date(), result.snapshotPath(),
                childRaw(root, "short_term_trend", JSON_NULL), childRaw(root, "current_situation", JSON_NULL),
                result.updated(), result.skipReason(), List.of(),
                saved == null || target == null ? null : snapshotDetail(target, saved));
    }

    private TargetView targetView(ApplicationAnalysisPageBO.Target target) {
        String targetKey = policy.defaultString(target.getTargetKey(), policy.buildTargetKey(target.getTargetType(), target.getSymbol()));
        return new TargetView(target, repository.findCurrentResult(targetKey) != null, policy.readStringList(target.getTagsJson()));
    }

    private ApplicationAnalysisPageBO.Target toTarget(TargetInput target) {
        String targetType = policy.defaultString(target.targetType(), "stock");
        String symbol = policy.defaultString(target.symbol(), "").toUpperCase(Locale.ROOT);
        String key = policy.buildTargetKey(targetType, symbol);
        return ApplicationAnalysisPageBO.Target.builder().targetKey(key)
                .legacyKey(policy.defaultString(target.id(), key)).targetType(targetType).symbol(symbol)
                .market(target.market()).name(policy.defaultString(target.name(), symbol))
                .adjust(policy.defaultString(target.adjust(), "qfq"))
                .enabled(target.enabled() == null || target.enabled()).intervalMinutes(target.intervalMinutes())
                .tagsJson(writeJson(target.tags() == null ? List.of() : target.tags(), EMPTY_ARRAY)).build();
    }

    private List<HistoryFile> historyFiles(String targetKey) {
        return repository.listHistory(targetKey, 20).stream().map(row -> new HistoryFile(row.getHistoryKey(),
                "postgres://cynexus_appl_ai.application_analysis_result_history/" + targetKey + "/" + row.getHistoryKey(),
                jsonSize(row.getPayloadJson()), row.getAnalysisRunAt() == null ? row.getUpdatedAt() : row.getAnalysisRunAt())).toList();
    }

    private SnapshotFile snapshotFile(String targetKey, ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        return new SnapshotFile(dailyFilename(snapshot), dailyPath(targetKey, snapshot), snapshot.getTradeDate(),
                jsonSize(snapshot.getPayloadJson()), snapshot.getUpdatedAt());
    }

    private SnapshotDetail snapshotDetail(ApplicationAnalysisPageBO.Target target, ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        JsonNode root = parseJson(snapshot.getPayloadJson());
        return new SnapshotDetail(target, snapshot, childRaw(root, "short_term_trend", JSON_NULL),
                childRaw(root, "current_situation", JSON_NULL), childRaw(root, "summary", JSON_NULL),
                policy.readStringList(target.getTagsJson()));
    }

    private String dailyFilename(ApplicationAnalysisPageBO.DailySnapshot snapshot) { return snapshot.getTradeDate() + ".json"; }

    private String dailyPath(String targetKey, ApplicationAnalysisPageBO.DailySnapshot snapshot) {
        return "postgres://cynexus_appl_ai.application_analysis_daily_snapshots/" + targetKey + "/" + dailyFilename(snapshot);
    }

    private String normalizeTargetKey(String value) { return policy.normalizeTargetKey(value); }

    private Integer jsonSize(String json) { return policy.defaultString(json, EMPTY_OBJECT).getBytes(StandardCharsets.UTF_8).length; }

    private JsonNode parseJson(String json) {
        try { return objectMapper.readTree(policy.defaultString(json, EMPTY_OBJECT)); }
        catch (JsonProcessingException exception) {
            try { return objectMapper.readTree(EMPTY_OBJECT); }
            catch (JsonProcessingException impossible) { throw new IllegalStateException(impossible); }
        }
    }

    private String childRaw(JsonNode node, String fieldName, String fallbackRaw) {
        return node == null || !node.has(fieldName) || node.get(fieldName).isMissingNode()
                ? fallbackRaw : node.get(fieldName).toString();
    }

    private String writeJson(Object value, String fallback) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { return fallback; }
    }
}
