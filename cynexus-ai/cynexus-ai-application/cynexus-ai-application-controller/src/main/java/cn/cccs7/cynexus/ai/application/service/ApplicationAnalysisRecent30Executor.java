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
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisSnapshotDomainPolicy;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ApplicationAnalysisSnapshotDomainPolicy snapshotPolicy = new ApplicationAnalysisSnapshotDomainPolicy();

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
        String targetKey = snapshotPolicy.normalizeTargetKey(targetId);
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
                AiCompletionResultBO completion = agentExecutor.completeDetailed(model, promptText, CAPABILITY);
                ObjectNode sanitized = snapshotPolicy.sanitize(
                        snapshotPolicy.readObject(structuredJsonExtractor.extractObject(completion.getContent())),
                        target, analysisInput);
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
        rawMeta.put("raw_response_preview", snapshotPolicy.truncate(completion.getRawResponse(), MAX_LOG_TEXT_LENGTH));
        payload.set("raw_meta", rawMeta);
        return payload;
    }

    private SnapshotWriteResult writeDailySnapshot(ApplicationAnalysisPageBO.Target target,
                                                  ObjectNode runPayload,
                                                  LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? snapshotPolicy.latestTradeDate(runPayload) : requestedDate;
        String targetKey = snapshotPolicy.normalizeTargetKey(target.getTargetKey());
        String snapshotPath = snapshotPolicy.dailyPath(targetKey, date);
        ApplicationAnalysisSnapshotDomainPolicy.SnapshotFacts probe = snapshotPolicy.mergeSnapshot(
                target, runPayload, objectMapper.createObjectNode(), requestedDate, OffsetDateTime.now(SHANGHAI));
        if (!probe.updated()) return new SnapshotWriteResult(date, snapshotPath, false, probe.skipReason(), null);

        ApplicationAnalysisPageBO.DailySnapshot existing = repository.findDailySnapshot(targetKey, date);
        ObjectNode existingPayload = existing == null ? objectMapper.createObjectNode() : snapshotPolicy.readObject(existing.getPayloadJson());
        ApplicationAnalysisSnapshotDomainPolicy.SnapshotFacts facts = snapshotPolicy.mergeSnapshot(
                target, runPayload, existingPayload, requestedDate, OffsetDateTime.now(SHANGHAI));
        ObjectNode serialized = facts.payload();

        ApplicationAnalysisPageBO.DailySnapshot saved = repository.upsertDailySnapshot(
                ApplicationAnalysisPageBO.DailySnapshotUpsertCommand.builder()
                        .targetId(target.getId())
                        .targetKey(targetKey)
                        .tradeDate(facts.date())
                        .snapshotKind("recent30")
                        .payloadJson(serialized.toString())
                        .remark("application_recent30")
                        .build());
        return new SnapshotWriteResult(facts.date(), snapshotPolicy.dailyPath(targetKey, saved.getTradeDate()), true, null, serialized);
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
                    .inputTokens(completion == null ? snapshotPolicy.estimateTokens(inputJson) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? snapshotPolicy.estimateTokens(inputJson) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("analysisInputPreview", snapshotPolicy.truncate(inputJson, MAX_LOG_TEXT_LENGTH))))
                    .responseJson(toJson(Map.of("contentPreview",
                            snapshotPolicy.truncate(completion == null ? "" : completion.getContent(), MAX_LOG_TEXT_LENGTH))))
                    .errorMessage(error == null ? null : snapshotPolicy.truncate(error.getMessage(), 2000))
                    .extra("{}")
                    .build());
        } catch (Exception ignored) {
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
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
