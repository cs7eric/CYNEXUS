package cn.cccs7.cynexus.ai.domain.applicationanalysis.impl;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisCompletionPort;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisSnapshotDomainPolicy;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisStructuredOutputPort;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the recent-30 analysis decision flow. External calls are ports; routing,
 * validation, fallback, snapshot merging and result state are domain policy.
 */
public class ApplicationAnalysisExecutionDomainServiceImpl
        implements ApplicationAnalysisExecutionDomainService {

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
    private final ApplicationAnalysisRoutingDomainService routingService;
    private final ApplicationAnalysisCompletionPort completionPort;
    private final ApplicationAnalysisStructuredOutputPort structuredOutputPort;
    private final AiPromptRepository promptRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final ApplicationAnalysisSnapshotDomainPolicy snapshotPolicy;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public ApplicationAnalysisExecutionDomainServiceImpl(
            ApplicationAnalysisPageRepository repository,
            ApplicationAnalysisRoutingDomainService routingService,
            ApplicationAnalysisCompletionPort completionPort,
            ApplicationAnalysisStructuredOutputPort structuredOutputPort,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository) {
        this.repository = repository;
        this.routingService = routingService;
        this.completionPort = completionPort;
        this.structuredOutputPort = structuredOutputPort;
        this.promptRepository = promptRepository;
        this.executionLogRepository = executionLogRepository;
        this.snapshotPolicy = new ApplicationAnalysisSnapshotDomainPolicy();
    }

    @Override
    public ApplicationAnalysisPageBO.Target requireTarget(String targetId) {
        String targetKey = snapshotPolicy.normalizeTargetKey(targetId);
        ApplicationAnalysisPageBO.Target target = repository.findTarget(targetKey);
        if (target == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "target " + targetKey + " not found");
        }
        return target;
    }

    @Override
    public ExecutionResult execute(ExecutionCommand command) {
        if (command == null || command.target() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "application-analysis target is required");
        }
        String targetKey = snapshotPolicy.normalizeTargetKey(command.target().getTargetKey());
        Map<String, Object> analysisInput = command.analysisInput() == null ? Map.of() : command.analysisInput();
        PromptRuntime prompt = resolvePrompt();
        String promptText = renderPrompt(prompt, analysisInput);
        List<AiModelBO> models = routingService.routeModels(CAPABILITY, null);
        if (models == null || models.isEmpty()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Application recent30 AI failed: no available model");
        }

        Exception lastException = null;
        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = completionPort.complete(model, promptText, CAPABILITY);
                ObjectNode sanitized = validateAndSanitize(completion, command.target(), analysisInput);
                ObjectNode payload = buildRunPayload(analysisInput, sanitized, completion);
                SnapshotWriteResult snapshot = writeDailySnapshot(command.target(), payload, command.requestedDate());
                persistRun(command.target(), payload, snapshot);
                recordExecutionLog(executionNo, prompt, model, "SUCCEEDED", startedAt, analysisInput, completion, null);
                return new ExecutionResult(true, targetKey, snapshot.date(), snapshot.snapshotPath(), snapshot.updated(),
                        snapshot.skipReason(), payload);
            } catch (Exception exception) {
                lastException = exception;
                recordExecutionLog(executionNo, prompt, model, "FAILED", startedAt, analysisInput, null, exception);
            }
        }
        String reason = lastException == null ? "no available model" : lastException.getMessage();
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Application recent30 AI failed: " + reason);
    }

    private ObjectNode validateAndSanitize(AiCompletionResultBO completion,
                                            ApplicationAnalysisPageBO.Target target,
                                            Map<String, Object> analysisInput) {
        if (completion == null || completion.getContent() == null || completion.getContent().isBlank()) {
            throw new IllegalStateException("AI returned an empty completion");
        }
        String extracted = structuredOutputPort.extractObject(completion.getContent());
        ObjectNode root = snapshotPolicy.readObject(extracted);
        if (!root.has("analysis_result") && !hasLegacyResultFields(root)) {
            throw new IllegalStateException("AI response does not contain analysis_result");
        }
        ObjectNode sanitized = snapshotPolicy.sanitize(root, target, analysisInput);
        if (!sanitized.path("analysis_result").isObject()) {
            throw new IllegalStateException("AI response contains an invalid analysis_result");
        }
        return sanitized;
    }

    private boolean hasLegacyResultFields(ObjectNode root) {
        return root.has("target") || root.has("data_quality") || root.has("short_term_trend")
                || root.has("current_situation");
    }

    private PromptRuntime resolvePrompt() {
        AiUsageBindingBO binding = routingService.findBinding(CAPABILITY);
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO boundPrompt = promptRepository.findById(binding.getPromptId());
            if (boundPrompt != null && "active".equalsIgnoreCase(boundPrompt.getStatus())) {
                return prompt(boundPrompt);
            }
        }
        String promptVersion = routingService.promptVersion(binding);
        List<AiPromptBO> prompts = promptRepository.list(PROMPT_KEY, null, "active").stream()
                .filter(item -> PROMPT_KEY.equals(item.getPromptKey()))
                .filter(item -> promptVersion == null || promptVersion.isBlank()
                        || promptVersion.equals(item.getPromptVersion()))
                .toList();
        return prompts.isEmpty() ? new PromptRuntime(PROMPT_KEY, "fallback-service-default",
                DEFAULT_SYSTEM_PROMPT, DEFAULT_USER_PROMPT_TEMPLATE) : prompt(prompts.getFirst());
    }

    private PromptRuntime prompt(AiPromptBO prompt) {
        return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(),
                defaultString(prompt.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT),
                defaultString(prompt.getUserPromptTemplate(), DEFAULT_USER_PROMPT_TEMPLATE));
    }

    private String renderPrompt(PromptRuntime prompt, Map<String, Object> input) {
        String inputJson = toJson(input);
        return prompt.systemPrompt() + System.lineSeparator()
                + prompt.userPromptTemplate().replace("{{analysis_input}}", inputJson);
    }

    private ObjectNode buildRunPayload(Map<String, Object> input, ObjectNode sanitized,
                                       AiCompletionResultBO completion) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("analysis_input", objectMapper.valueToTree(input));
        payload.set("analysis_result", sanitized.path("analysis_result"));
        payload.set("raw_result", sanitized);
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("provider_code", completion.getProviderCode());
        meta.put("model_code", completion.getModelCode());
        meta.put("raw_response_preview", snapshotPolicy.truncate(completion.getRawResponse(), MAX_LOG_TEXT_LENGTH));
        payload.set("raw_meta", meta);
        return payload;
    }

    private SnapshotWriteResult writeDailySnapshot(ApplicationAnalysisPageBO.Target target,
                                                   ObjectNode runPayload, LocalDate requestedDate) {
        LocalDate date = requestedDate == null ? snapshotPolicy.latestTradeDate(runPayload) : requestedDate;
        String targetKey = snapshotPolicy.normalizeTargetKey(target.getTargetKey());
        ApplicationAnalysisSnapshotDomainPolicy.SnapshotFacts probe = snapshotPolicy.mergeSnapshot(
                target, runPayload, objectMapper.createObjectNode(), requestedDate, OffsetDateTime.now(SHANGHAI));
        if (!probe.updated()) {
            return new SnapshotWriteResult(date, snapshotPolicy.dailyPath(targetKey, date), false,
                    probe.skipReason());
        }
        ApplicationAnalysisPageBO.DailySnapshot existing = repository.findDailySnapshot(targetKey, date);
        ObjectNode existingPayload = existing == null ? objectMapper.createObjectNode()
                : snapshotPolicy.readObject(existing.getPayloadJson());
        ApplicationAnalysisSnapshotDomainPolicy.SnapshotFacts facts = snapshotPolicy.mergeSnapshot(
                target, runPayload, existingPayload, requestedDate, OffsetDateTime.now(SHANGHAI));
        ApplicationAnalysisPageBO.DailySnapshot saved = repository.upsertDailySnapshot(
                ApplicationAnalysisPageBO.DailySnapshotUpsertCommand.builder()
                        .targetId(target.getId()).targetKey(targetKey).tradeDate(facts.date())
                        .snapshotKind("recent30").payloadJson(facts.payload().toString())
                        .remark("application_recent30").build());
        return new SnapshotWriteResult(facts.date(), snapshotPolicy.dailyPath(targetKey, saved.getTradeDate()),
                true, null);
    }

    private void persistRun(ApplicationAnalysisPageBO.Target target, ObjectNode payload,
                            SnapshotWriteResult snapshot) {
        OffsetDateTime now = OffsetDateTime.now(SHANGHAI);
        repository.upsertCurrentResult(ApplicationAnalysisPageBO.CurrentResultUpsertCommand.builder()
                .targetId(target.getId()).targetKey(target.getTargetKey()).status("success")
                .analysisRunAt(now).payloadJson(payload.toString()).remark(CAPABILITY).build());
        repository.createHistoryResult(ApplicationAnalysisPageBO.HistoryResultCreateCommand.builder()
                .targetId(target.getId()).targetKey(target.getTargetKey())
                .historyKey("recent30-" + snapshot.date()).sourceKind(CAPABILITY)
                .analysisRunAt(now).payloadJson(payload.toString()).remark(snapshot.skipReason()).build());
    }

    private void recordExecutionLog(String executionNo, PromptRuntime prompt, AiModelBO model,
                                    String status, Instant startedAt, Map<String, Object> input,
                                    AiCompletionResultBO completion, Exception error) {
        try {
            String inputJson = toJson(input);
            executionLogRepository.create(AiExecutionLogBO.builder().executionNo(executionNo)
                    .capability(CAPABILITY).promptKey(prompt.promptKey()).promptVersion(prompt.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode()).status(status)
                    .inputTokens(completion == null ? snapshotPolicy.estimateTokens(inputJson) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? snapshotPolicy.estimateTokens(inputJson) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(previewJson("analysisInputPreview", snapshotPolicy.truncate(inputJson, MAX_LOG_TEXT_LENGTH)))
                    .responseJson(previewJson("contentPreview", snapshotPolicy.truncate(
                            completion == null ? "" : completion.getContent(), MAX_LOG_TEXT_LENGTH)))
                    .errorMessage(error == null ? null : snapshotPolicy.truncate(error.getMessage(), 2000))
                    .extra("{}").build());
        } catch (Exception ignored) {
            // Observability failure must not change the model fallback decision.
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String previewJson(String field, String value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(field, value == null ? "" : value);
        return node.toString();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record PromptRuntime(String promptKey, String promptVersion,
                                 String systemPrompt, String userPromptTemplate) {
    }

    private record SnapshotWriteResult(LocalDate date, String snapshotPath,
                                       boolean updated, String skipReason) {
    }
}
