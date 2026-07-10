package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationAnalysisRecent30Executor {

    public static final String CAPABILITY = "application_recent30";
    public static final String PROMPT_KEY = "application_recent30_short_term_daily";
    private static final int MAX_LOG_TEXT_LENGTH = 8000;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个专注的 A 股 K 线短期趋势研判模型。
            基于 analysis_input 输出唯一 JSON 对象，根字段只能是 analysis_result。
            analysis_result 必须包含 target、data_quality、short_term_trend、current_situation。
            严禁输出投资建议、买卖点、仓位、目标价、Markdown、解释、<think> 或 reasoning_content。
            """;
    private static final String DEFAULT_USER_PROMPT_TEMPLATE = """
            分析输入 JSON：
            {{analysis_input}}
            请输出严格 JSON；根字段只能出现 analysis_result。
            """;

    private final ApplicationAnalysisPageRepository repository;
    private final ApplicationAnalysisKlineInputBuilder inputBuilder;
    private final ApplicationAnalysisModelRouter modelRouter;
    private final AgentExecutor agentExecutor;
    private final AiPromptRepository aiPromptRepository;
    private final AiExecutionLogRepository aiExecutionLogRepository;
    private final StructuredJsonExtractor structuredJsonExtractor;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisRecent30Executor(ApplicationAnalysisPageRepository repository,
                                               ApplicationAnalysisKlineInputBuilder inputBuilder,
                                               ApplicationAnalysisModelRouter modelRouter,
                                               AgentExecutor agentExecutor,
                                               AiPromptRepository aiPromptRepository,
                                               AiExecutionLogRepository aiExecutionLogRepository,
                                               StructuredJsonExtractor structuredJsonExtractor) {
        this.repository = repository;
        this.inputBuilder = inputBuilder;
        this.modelRouter = modelRouter;
        this.agentExecutor = agentExecutor;
        this.aiPromptRepository = aiPromptRepository;
        this.aiExecutionLogRepository = aiExecutionLogRepository;
        this.structuredJsonExtractor = structuredJsonExtractor;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Transactional
    public ExecutionResult execute(String targetId, LocalDate date) {
        String targetKey = normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "target " + targetKey + " not found");
        }
        Map<String, Object> analysisInput = inputBuilder.buildRecent30Input(target);
        ApplicationAnalysisPromptRuntime prompt = resolvePrompt();
        String promptText = renderPrompt(prompt, analysisInput);
        List<AiModelBO> models = modelRouter.routeModels(CAPABILITY, null);
        Exception lastException = null;

        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = agentExecutor.completeDetailed(model, promptText);
                ObjectNode sanitized = sanitizeShortTerm(completion.getContent(), target, analysisInput);
                ObjectNode payload = buildRunPayload(analysisInput, sanitized, completion);
                SnapshotWriteResult snapshot = writeDailySnapshot(target, payload, date);
                persistRun(target, payload, snapshot, "success");
                recordExecutionLog(executionNo, prompt, model, "SUCCEEDED", startedAt, analysisInput, completion, null);
                return new ExecutionResult(true, targetKey, snapshot.date(), snapshot.snapshotPath(), snapshot.updated(),
                        snapshot.skipReason(), payload);
            } catch (Exception exception) {
                lastException = exception;
                recordExecutionLog(executionNo, prompt, model, "FAILED", startedAt, analysisInput, null, exception);
            }
        }

        throw new BizException(ErrorCode.INTERNAL_ERROR,
                "Application recent30 AI failed: "
                        + (lastException == null ? "no available model" : lastException.getMessage()));
    }

    private ApplicationAnalysisPromptRuntime resolvePrompt() {
        AiUsageBindingBO binding = modelRouter.findBinding(CAPABILITY);
        String promptVersion = modelRouter.promptVersion(binding);
        List<AiPromptBO> prompts = aiPromptRepository.list(PROMPT_KEY, null, "active").stream()
                .filter(prompt -> PROMPT_KEY.equals(prompt.getPromptKey()))
                .filter(prompt -> promptVersion.isBlank() || promptVersion.equals(prompt.getPromptVersion()))
                .toList();
        if (!prompts.isEmpty()) {
            AiPromptBO prompt = prompts.getFirst();
            return new ApplicationAnalysisPromptRuntime(
                    prompt.getPromptKey(),
                    prompt.getPromptVersion(),
                    defaultString(prompt.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT),
                    defaultString(prompt.getUserPromptTemplate(), DEFAULT_USER_PROMPT_TEMPLATE)
            );
        }
        return new ApplicationAnalysisPromptRuntime(PROMPT_KEY, "fallback-service-default",
                DEFAULT_SYSTEM_PROMPT, DEFAULT_USER_PROMPT_TEMPLATE);
    }

    private String renderPrompt(ApplicationAnalysisPromptRuntime prompt, Map<String, Object> analysisInput) {
        String analysisInputJson = toJson(analysisInput);
        String userPrompt = prompt.userPromptTemplate().replace("{{analysis_input}}", analysisInputJson);
        return prompt.systemPrompt() + System.lineSeparator() + userPrompt;
    }

    private ObjectNode sanitizeShortTerm(String rawContent,
                                         ApplicationAnalysisPageBO.Target target,
                                         Map<String, Object> analysisInput) {
        ObjectNode rawRoot = readObject(structuredJsonExtractor.extractObject(rawContent));
        ObjectNode analysisResult;
        JsonNode fromRoot = rawRoot.get("analysis_result");
        if (fromRoot != null && fromRoot.isObject()) {
            analysisResult = (ObjectNode) fromRoot;
        } else {
            analysisResult = objectMapper.createObjectNode();
            copyIfPresent(rawRoot, analysisResult, "target");
            copyIfPresent(rawRoot, analysisResult, "data_quality");
            copyIfPresent(rawRoot, analysisResult, "short_term_trend");
            copyIfPresent(rawRoot, analysisResult, "current_situation");
        }

        ObjectNode cleaned = objectMapper.createObjectNode();
        JsonNode targetNode = analysisResult.get("target");
        cleaned.set("target", targetNode != null && targetNode.isObject() ? targetNode : targetNode(target));
        JsonNode qualityNode = analysisResult.get("data_quality");
        cleaned.set("data_quality", normalizeDataQuality(qualityNode, analysisInput));
        JsonNode shortTerm = analysisResult.get("short_term_trend");
        JsonNode situation = analysisResult.get("current_situation");
        cleaned.set("short_term_trend", shortTerm != null && shortTerm.isObject() ? shortTerm : nullNode());
        cleaned.set("current_situation", situation != null && situation.isObject() ? situation : nullNode());
        addMissingWarnings(cleaned);

        ObjectNode sanitized = objectMapper.createObjectNode();
        sanitized.set("analysis_result", cleaned);
        return sanitized;
    }

    private ObjectNode buildRunPayload(Map<String, Object> analysisInput,
                                       ObjectNode sanitized,
                                       AiCompletionResultBO completion) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("analysis_input", objectMapper.valueToTree(analysisInput));
        payload.set("analysis_result", sanitized.path("analysis_result"));
        payload.set("raw_result", sanitized);
        ObjectNode rawMeta = objectMapper.createObjectNode();
        rawMeta.put("provider_code", completion.getProviderCode());
        rawMeta.put("model_code", completion.getModelCode());
        rawMeta.put("raw_response_preview", truncate(completion.getRawResponse(), MAX_LOG_TEXT_LENGTH));
        payload.set("raw_meta", rawMeta);
        return payload;
    }

    private SnapshotWriteResult writeDailySnapshot(ApplicationAnalysisPageBO.Target target,
                                                  ObjectNode runPayload,
                                                  LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? latestTradeDate(runPayload) : requestedDate;
        ObjectNode newAnalysis = copyObject(runPayload.path("analysis_result"));
        JsonNode newShort = newAnalysis.get("short_term_trend");
        JsonNode newSituation = newAnalysis.get("current_situation");
        JsonNode newSummary = newAnalysis.get("summary");
        boolean hasNewData = isMeaningful(newShort) || isMeaningful(newSituation) || isMeaningful(newSummary);
        String targetKey = normalizeTargetKey(target.getTargetKey());
        String snapshotPath = dailyPath(targetKey, date);
        if (!hasNewData) {
            return new SnapshotWriteResult(date, snapshotPath, false, "no_new_data", null);
        }

        ApplicationAnalysisPageBO.DailySnapshot existing = repository.findDailySnapshot(targetKey, date);
        ObjectNode existingPayload = existing == null ? objectMapper.createObjectNode() : readObject(existing.getPayloadJson());
        ObjectNode serialized = existingPayload.deepCopy();
        serialized.set("target", targetNode(target));
        serialized.put("date", date.toString());
        serialized.put("updated_at", OffsetDateTime.now(SHANGHAI).toString());
        serialized.set("recent_window", runPayload.path("analysis_input").path("recent_window"));
        setIfMeaningful(serialized, "short_term_trend", newShort, existingPayload);
        setIfMeaningful(serialized, "current_situation", newSituation, existingPayload);
        setIfMeaningful(serialized, "summary", newSummary, existingPayload);
        ObjectNode mergedAnalysis = deepMerge(copyObject(existingPayload.path("analysis_result")), newAnalysis);
        mergeWarnings(mergedAnalysis, existingPayload.path("analysis_result"), newAnalysis);
        serialized.set("analysis_result", mergedAnalysis);

        ApplicationAnalysisPageBO.DailySnapshot saved = repository.upsertDailySnapshot(
                ApplicationAnalysisPageBO.DailySnapshotUpsertCommand.builder()
                        .targetId(target.getId())
                        .targetKey(targetKey)
                        .tradeDate(date)
                        .snapshotKind("recent30")
                        .payloadJson(serialized.toString())
                        .remark("application_recent30")
                        .build());
        return new SnapshotWriteResult(date, dailyPath(targetKey, saved.getTradeDate()), true, null, serialized);
    }

    private void persistRun(ApplicationAnalysisPageBO.Target target,
                            ObjectNode runPayload,
                            SnapshotWriteResult snapshot,
                            String status) {
        String payload = runPayload.toString();
        OffsetDateTime now = OffsetDateTime.now(SHANGHAI);
        repository.upsertCurrentResult(ApplicationAnalysisPageBO.CurrentResultUpsertCommand.builder()
                .targetId(target.getId())
                .targetKey(target.getTargetKey())
                .status(status)
                .analysisRunAt(now)
                .payloadJson(payload)
                .remark(CAPABILITY)
                .build());
        repository.createHistoryResult(ApplicationAnalysisPageBO.HistoryResultCreateCommand.builder()
                .targetId(target.getId())
                .targetKey(target.getTargetKey())
                .historyKey("recent30-" + snapshot.date())
                .sourceKind(CAPABILITY)
                .analysisRunAt(now)
                .payloadJson(payload)
                .remark(snapshot.skipReason())
                .build());
    }

    private void recordExecutionLog(String executionNo,
                                    ApplicationAnalysisPromptRuntime prompt,
                                    AiModelBO model,
                                    String status,
                                    Instant startedAt,
                                    Map<String, Object> analysisInput,
                                    AiCompletionResultBO completion,
                                    Exception error) {
        try {
            String inputJson = toJson(analysisInput);
            aiExecutionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo(executionNo)
                    .capability(CAPABILITY)
                    .promptKey(prompt.promptKey())
                    .promptVersion(prompt.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status(status)
                    .inputTokens(completion == null ? estimateTokens(inputJson) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? estimateTokens(inputJson) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("analysisInputPreview", truncate(inputJson, MAX_LOG_TEXT_LENGTH))))
                    .responseJson(toJson(Map.of("contentPreview",
                            truncate(completion == null ? "" : completion.getContent(), MAX_LOG_TEXT_LENGTH))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000))
                    .extra("{}")
                    .build());
        } catch (Exception ignored) {
        }
    }

    private ObjectNode normalizeDataQuality(JsonNode qualityNode, Map<String, Object> analysisInput) {
        ObjectNode quality = qualityNode != null && qualityNode.isObject()
                ? copyObject(qualityNode)
                : objectMapper.createObjectNode();
        if (!quality.has("daily_bars_count")) {
            quality.put("daily_bars_count", dailyBarsCount(analysisInput));
        }
        if (!quality.has("warnings") || !quality.get("warnings").isArray()) {
            quality.set("warnings", objectMapper.createArrayNode());
        }
        return quality;
    }

    private void addMissingWarnings(ObjectNode cleaned) {
        ArrayNode warnings = (ArrayNode) cleaned.path("data_quality").path("warnings");
        if (!cleaned.path("short_term_trend").isObject()) {
            warnings.add("AI 未返回 short_term_trend");
        }
        if (!cleaned.path("current_situation").isObject()) {
            warnings.add("AI 未返回 current_situation");
        }
    }

    private int dailyBarsCount(Map<String, Object> analysisInput) {
        try {
            Map<?, ?> bars = (Map<?, ?>) analysisInput.get("bars");
            Map<?, ?> daily = (Map<?, ?>) bars.get("daily");
            Object items = daily.get("items");
            return items instanceof List<?> list ? list.size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private LocalDate latestTradeDate(ObjectNode runPayload) {
        JsonNode items = runPayload.path("analysis_input").path("bars").path("daily").path("items");
        if (items.isArray() && !items.isEmpty()) {
            long timestamp = items.get(items.size() - 1).path("timestamp").asLong(0);
            if (timestamp > 0) {
                return java.time.Instant.ofEpochMilli(timestamp).atZone(SHANGHAI).toLocalDate();
            }
        }
        return LocalDate.now(SHANGHAI);
    }

    private ObjectNode targetNode(ApplicationAnalysisPageBO.Target target) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", target.getTargetKey());
        node.put("target_type", defaultString(target.getTargetType(), "stock"));
        node.put("symbol", defaultString(target.getSymbol(), ""));
        node.put("name", defaultString(target.getName(), target.getSymbol()));
        node.put("adjust", defaultString(target.getAdjust(), "qfq"));
        return node;
    }

    private ObjectNode deepMerge(ObjectNode base, ObjectNode overlay) {
        overlay.fields().forEachRemaining(entry -> {
            JsonNode existing = base.get(entry.getKey());
            JsonNode value = entry.getValue();
            if (existing != null && existing.isObject() && value.isObject()) {
                base.set(entry.getKey(), deepMerge(copyObject(existing), copyObject(value)));
            } else {
                base.set(entry.getKey(), value);
            }
        });
        return base;
    }

    private void mergeWarnings(ObjectNode mergedAnalysis, JsonNode existingAnalysis, JsonNode newAnalysis) {
        ObjectNode dataQuality = mergedAnalysis.path("data_quality").isObject()
                ? copyObject(mergedAnalysis.path("data_quality"))
                : objectMapper.createObjectNode();
        ArrayNode warnings = objectMapper.createArrayNode();
        addWarnings(warnings, existingAnalysis.path("data_quality").path("warnings"));
        addWarnings(warnings, newAnalysis.path("data_quality").path("warnings"));
        dataQuality.set("warnings", warnings);
        mergedAnalysis.set("data_quality", dataQuality);
    }

    private void addWarnings(ArrayNode target, JsonNode source) {
        if (!source.isArray()) {
            return;
        }
        for (JsonNode item : source) {
            String warning = item.asText("");
            boolean exists = false;
            for (JsonNode current : target) {
                if (warning.equals(current.asText())) {
                    exists = true;
                    break;
                }
            }
            if (!warning.isBlank() && !exists) {
                target.add(warning);
            }
        }
    }

    private void setIfMeaningful(ObjectNode target, String field, JsonNode newValue, ObjectNode existingPayload) {
        if (isMeaningful(newValue)) {
            target.set(field, newValue);
        } else if (existingPayload.has(field)) {
            target.set(field, existingPayload.get(field));
        }
    }

    private boolean isMeaningful(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return false;
        }
        if (value.isObject() || value.isArray()) {
            return !value.isEmpty();
        }
        return !value.asText("").isBlank();
    }

    private void copyIfPresent(ObjectNode source, ObjectNode target, String field) {
        if (source.has(field)) {
            target.set(field, source.get(field));
        }
    }

    private ObjectNode readObject(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(defaultString(rawJson, "{}"));
            return root != null && root.isObject() ? (ObjectNode) root : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private ObjectNode copyObject(JsonNode node) {
        return node != null && node.isObject() ? ((ObjectNode) node).deepCopy() : objectMapper.createObjectNode();
    }

    private JsonNode nullNode() {
        return objectMapper.nullNode();
    }

    private String dailyPath(String targetKey, LocalDate date) {
        return "postgres://cynexus_appl_ai.application_analysis_daily_snapshots/"
                + targetKey + "/" + date + ".json";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String normalizeTargetKey(String value) {
        return defaultString(value, "").toLowerCase();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ExecutionResult(boolean ok,
                                  String targetKey,
                                  LocalDate date,
                                  String snapshotPath,
                                  boolean updated,
                                  String skipReason,
                                  ObjectNode payload) {
    }

    private record SnapshotWriteResult(LocalDate date,
                                       String snapshotPath,
                                       boolean updated,
                                       String skipReason,
                                       ObjectNode snapshotPayload) {
    }
}
