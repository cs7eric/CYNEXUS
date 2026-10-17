package cn.cccs7.cynexus.ai.domain.futuresposition.impl;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainPolicy;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import cn.cccs7.cynexus.ai.domain.validation.BoundedStructuredOutputRepairPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import cn.cccs7.cynexus.common.core.constant.TraceId;

public class FuturesPositionAiDomainServiceImpl implements FuturesPositionAiDomainService {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是 Cynexus 的期货会员持仓结构研究助手，只分析 IF、IH、IM、IC 四个中金所股指期货品种。
            你只能使用输入中的当前快照、历史序列和会员排名，不得补充外部行情、新闻或未经输入支持的因果关系。
            你的职责是解释事实：多空结构、历史位置、变化持续性、会员增减仓证据和需要继续观察的条件。
            严禁输出买入、卖出、做多、做空、止损、目标价、仓位或收益预测。不要给品种打分，也不要用“主力”“庄家”等不可验证的词。
            只输出简体中文 JSON，不要输出 Markdown、代码围栏、思考过程或 JSON 之外的文字。
            """;
    private static final String DEFAULT_USER_PROMPT = """
            请基于以下持仓数据输出结构化分析：
            {{analysis_input}}
            """;

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiExecutionLogRepository executionLogRepository;
    private final FuturesPositionAiCompletionPort completionPort;
    private final AiPromptRuntimeResolver promptRuntimeResolver;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FuturesPositionAiDomainPolicy policy = new FuturesPositionAiDomainPolicy();
    private final AiStructuredOutputParser structuredOutputParser;

    public FuturesPositionAiDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                              AiExecutionLogRepository executionLogRepository,
                                              FuturesPositionAiCompletionPort completionPort,
                                              AiPromptRuntimeResolver promptRuntimeResolver) {
        this(routingService, executionLogRepository, completionPort, promptRuntimeResolver,
                AiStructuredOutputParser.shared());
    }

    public FuturesPositionAiDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                              AiExecutionLogRepository executionLogRepository,
                                              FuturesPositionAiCompletionPort completionPort,
                                              AiPromptRuntimeResolver promptRuntimeResolver,
                                              AiStructuredOutputParser structuredOutputParser) {
        this.routingService = routingService;
        this.executionLogRepository = executionLogRepository;
        this.completionPort = completionPort;
        this.promptRuntimeResolver = java.util.Objects.requireNonNull(promptRuntimeResolver, "promptRuntimeResolver");
        this.structuredOutputParser = java.util.Objects.requireNonNull(structuredOutputParser, "structuredOutputParser is required");
    }

    @Override
    public AnalysisResult analyze(AnalysisCommand command) {
        if (command == null) throw new IllegalArgumentException("期货持仓分析输入不能为空");
        String varietyCode = policy.requireAllowedVariety(command.varietyCode(), command.instrumentCode());
        Map<String, Object> input = policy.buildInput(command, varietyCode);
        String inputJson = toJson(input);
        if (inputJson.length() > 240_000) {
            throw new IllegalArgumentException("期货持仓分析数据过大，请缩小历史范围后重试");
        }
        PromptRuntime prompt = resolvePrompt();
        String promptText = render(prompt, inputJson);
        List<AiModelBO> models = routingService.routeModels(CAPABILITY, null);
        if (models == null || models.isEmpty()) {
            throw new IllegalStateException("未配置可用的期货持仓 AI provider");
        }

        Exception last = null;
        for (AiModelBO model : models) {
            Instant startedAt = Instant.now();
            String executionNo = "FPA-" + UUID.randomUUID().toString().replace("-", "");
            try {
                AiExecutionContext context = new AiExecutionContext(executionNo, CAPABILITY, prompt.promptKey(), prompt.promptVersion(),
                        model.getProviderCode(), model.getModelCode(), null, null, executionNo, TraceId.current());
                AiCompletionResultBO completion = completionPort.complete(model, promptText, context);
                ObjectNode analysis = parseAndValidate(completion == null ? null : completion.getContent(), model, context);
                recordExecution(executionNo, prompt, model, inputJson, completion, startedAt, null);
                return new AnalysisResult(varietyCode, value(command.instrumentCode()), value(command.tradeDate()), analysis,
                        completion.getProviderCode(), completion.getModelCode(), prompt.promptVersion());
            } catch (Exception exception) {
                last = exception;
                recordExecution(executionNo, prompt, model, inputJson, null, startedAt, exception);
            }
        }
        throw new IllegalStateException("期货持仓 AI 分析失败：" + (last == null ? "无可用模型" : last.getMessage()), last);
    }

    private PromptRuntime resolvePrompt() {
        var resolved = promptRuntimeResolver.resolve(CAPABILITY, PROMPT_KEY, DEFAULT_SYSTEM_PROMPT, DEFAULT_USER_PROMPT);
        return new PromptRuntime(resolved.promptKey(), resolved.promptVersion(), resolved);
    }

    private String render(PromptRuntime prompt, String inputJson) {
        return prompt.runtime().render(Map.of("analysis_input", inputJson));
    }

    private ObjectNode parseAndValidate(String content, AiModelBO model, AiExecutionContext context) throws Exception {
        var parsed = BoundedStructuredOutputRepairPolicy.parseOrRepair(content, null, structuredOutputParser, 32_000,
                raw -> completionPort.complete(model, promptRepair(raw), context.repairAttempt()).getContent());
        JsonNode root = objectMapper.readTree(parsed.json());
        JsonNode result = root.path("analysis_result");
        if (!result.isObject()) result = root.path("analysisResult");
        if (!result.isObject()) result = root;
        if (!result.isObject()) throw new IllegalStateException("AI 返回缺少 analysis_result");
        return normalizeResult((ObjectNode) result);
    }

    private String promptRepair(String raw) {
        return "修复以下模型输出为合法 JSON。只能保留原输出中有依据的字段，不得新增事实、交易建议或解释。"
                + "只返回 JSON，不要 Markdown。原始输出：\n" + truncate(raw, 24_000);
    }

    private ObjectNode normalizeResult(ObjectNode source) {
        ObjectNode normalized = objectMapper.createObjectNode();
        copyText(source, normalized, "headline", "headline", "title", "summary", "conclusion", "标题", "结论");
        copyText(source, normalized, "structure", "structure", "current_structure", "position_structure", "结构", "当前结构");
        copyList(source, normalized, "evidence", "evidence", "key_evidence", "basis", "证据", "依据");
        copyText(source, normalized, "trend", "trend", "change", "historical_change", "变化", "历史变化");
        copyText(source, normalized, "risk", "risk", "limitation", "limitations", "风险", "限制");
        copyList(source, normalized, "watch", "watch", "watch_items", "observation", "观察", "关注");
        copyText(source, normalized, "data_quality", "data_quality", "dataQuality", "数据质量");
        copyText(source, normalized, "disclaimer", "disclaimer", "note", "免责声明");

        if (!normalized.has("headline")) normalized.put("headline", "当前持仓结构已生成分析");
        if (!normalized.has("structure")) normalized.put("structure", "AI 未提供结构字段，请结合当前快照查看。");
        if (!normalized.has("evidence")) normalized.set("evidence", objectMapper.createArrayNode());
        if (!normalized.has("trend")) normalized.put("trend", "AI 未提供历史变化字段。");
        if (!normalized.has("risk")) normalized.put("risk", "AI 未提供风险提示。");
        if (!normalized.has("watch")) normalized.set("watch", objectMapper.createArrayNode());
        if (!normalized.has("data_quality")) normalized.put("data_quality", "基于当前快照、历史序列和会员排名。");
        if (!normalized.has("disclaimer")) normalized.put("disclaimer", "仅描述持仓事实，不构成交易建议。");
        return normalized;
    }

    private void copyText(ObjectNode source, ObjectNode target, String targetField, String... aliases) {
        JsonNode value = firstPresent(source, aliases);
        if (value == null || value.isNull()) return;
        if (value.isValueNode()) target.put(targetField, value.asText());
    }

    private void copyList(ObjectNode source, ObjectNode target, String targetField, String... aliases) {
        JsonNode value = firstPresent(source, aliases);
        if (value == null || value.isNull()) return;
        ArrayNode list = objectMapper.createArrayNode();
        if (value.isArray()) {
            value.forEach(item -> {
                if (item.isValueNode()) list.add(item.asText());
            });
        } else if (value.isValueNode()) {
            list.add(value.asText());
        }
        target.set(targetField, list);
    }

    private JsonNode firstPresent(ObjectNode source, String... aliases) {
        for (String alias : aliases) {
            JsonNode value = source.get(alias);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private void recordExecution(String executionNo, PromptRuntime prompt, AiModelBO model, String inputJson,
                                 AiCompletionResultBO completion, Instant startedAt, Exception error) {
        try {
            executionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo(executionNo).capability(CAPABILITY)
                    .promptKey(prompt.promptKey()).promptVersion(prompt.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status(error == null ? "SUCCEEDED" : "FAILED")
                    .inputTokens(completion == null ? 0 : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? 0 : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("analysisInput", truncate(inputJson, 4000))))
                    .responseJson(toJson(Map.of("contentPreview", truncate(completion == null ? "" : completion.getContent(), 4000))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000))
                    .extra("{\"rawResponseStored\":false}").build());
        } catch (Exception ignored) {
            // Observability failure must not change the completion outcome.
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String truncate(String value, int limit) {
        if (value == null) return "";
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record PromptRuntime(String promptKey, String promptVersion,
                                 AiPromptRuntimeResolver.AiPromptRuntime runtime) {
    }
}
