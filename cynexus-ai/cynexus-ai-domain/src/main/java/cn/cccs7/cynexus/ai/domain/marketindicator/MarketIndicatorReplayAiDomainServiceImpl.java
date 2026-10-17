package cn.cccs7.cynexus.ai.domain.marketindicator;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import cn.cccs7.cynexus.common.core.constant.TraceId;

public final class MarketIndicatorReplayAiDomainServiceImpl implements MarketIndicatorReplayAiDomainService {
    private static final String FALLBACK_SYSTEM = "你是 Cynexus A 股盘后复盘解释器。只能依据输入证据解释板块 Episode，不得补充外部事实，不得改写评分、时间、收益或数据质量，不得给出买入卖出、目标价、仓位或收益承诺。必须明确支持证据、反证与不确定性。只输出简体中文 JSON。";
    private static final String FALLBACK_USER = "请解释以下确定性复盘证据：{{evidence}}";

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiExecutionLogRepository executionLogRepository;
    private final MarketIndicatorReplayAiCompletionPort completionPort;
    private final AiPromptRuntimeResolver promptRuntimeResolver;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AiStructuredOutputParser structuredOutputParser;

    public MarketIndicatorReplayAiDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                                     AiExecutionLogRepository executionLogRepository,
                                                     MarketIndicatorReplayAiCompletionPort completionPort,
                                                     AiPromptRuntimeResolver promptRuntimeResolver) {
        this(routingService, executionLogRepository, completionPort, promptRuntimeResolver,
                AiStructuredOutputParser.shared());
    }

    public MarketIndicatorReplayAiDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                                     AiExecutionLogRepository executionLogRepository,
                                                     MarketIndicatorReplayAiCompletionPort completionPort,
                                                     AiPromptRuntimeResolver promptRuntimeResolver,
                                                     AiStructuredOutputParser structuredOutputParser) {
        this.routingService = routingService;
        this.executionLogRepository = executionLogRepository;
        this.completionPort = completionPort;
        this.promptRuntimeResolver = java.util.Objects.requireNonNull(promptRuntimeResolver, "promptRuntimeResolver");
        this.structuredOutputParser = java.util.Objects.requireNonNull(structuredOutputParser, "structuredOutputParser is required");
    }

    @Override
    public Map<String, Object> explain(Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) throw new IllegalArgumentException("复盘证据不能为空");
        String inputJson = toJson(limitEvidence(evidence));
        if (inputJson.length() > 180_000) throw new IllegalArgumentException("复盘证据过大，请先选择单个 Episode");
        PromptRuntime prompt = resolvePrompt();
        List<AiModelBO> models = routingService.routeModels(CAPABILITY, null);
        if (models == null || models.isEmpty()) throw new IllegalStateException("未配置可用的复盘解释 AI provider");
        Exception last = null;
        for (AiModelBO model : models) {
            Instant started = Instant.now();
            String executionNo = "MIR-" + UUID.randomUUID().toString().replace("-", "");
            try {
                AiExecutionContext context = new AiExecutionContext(executionNo, CAPABILITY, prompt.key(), prompt.version(),
                        model.getProviderCode(), model.getModelCode(), null, null, executionNo, TraceId.current());
                AiCompletionResultBO completion = completionPort.complete(model, render(prompt, inputJson), context);
                Map<String, Object> normalized = normalize(parse(completion == null ? null : completion.getContent(), model, context), inputJson);
                normalized.put("status", "ai");
                normalized.put("providerCode", completion.getProviderCode());
                normalized.put("modelCode", completion.getModelCode());
                normalized.put("promptVersion", prompt.version());
                record(executionNo, prompt, model, inputJson, completion, started, null);
                return normalized;
            } catch (Exception exception) {
                last = exception;
                record(executionNo, prompt, model, inputJson, null, started, exception);
            }
        }
        throw new IllegalStateException("复盘解释 AI 调用失败：" + (last == null ? "无可用模型" : last.getMessage()), last);
    }

    private Map<String, Object> limitEvidence(Map<String, Object> evidence) {
        Map<String, Object> copy = new LinkedHashMap<>(evidence);
        Object timeline = copy.get("timeline");
        if (timeline instanceof Map<?, ?> map) {
            Map<String, Object> bounded = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if ("indexPoints".equals(String.valueOf(key)) && value instanceof List<?> list) bounded.put(String.valueOf(key), list.stream().limit(240).toList());
                else if ("events".equals(String.valueOf(key)) && value instanceof List<?> list) bounded.put(String.valueOf(key), list.stream().limit(80).toList());
                else bounded.put(String.valueOf(key), value);
            });
            copy.put("timeline", bounded);
        }
        return copy;
    }

    private PromptRuntime resolvePrompt() {
        var resolved = promptRuntimeResolver.resolve(CAPABILITY, PROMPT_KEY, FALLBACK_SYSTEM, FALLBACK_USER);
        return new PromptRuntime(resolved.promptKey(), resolved.promptVersion(), resolved.systemPrompt(), resolved.userPromptTemplate(), resolved);
    }

    private String render(PromptRuntime prompt, String evidence) {
        return prompt.runtime().render(Map.of("evidence", evidence));
    }

    private ObjectNode parse(String content, AiModelBO model, AiExecutionContext context) throws Exception {
        var parsed = BoundedStructuredOutputRepairPolicy.parseOrRepair(content, null, structuredOutputParser, 32_000,
                raw -> completionPort.complete(model,
                        "修复以下复盘解释输出为合法 JSON，只返回 JSON，不得添加输入中没有的事实：\n"
                                + truncate(raw, 24_000), context.repairAttempt()).getContent());
        JsonNode root = objectMapper.readTree(parsed.json());
        if (!root.isObject()) throw new IllegalStateException("AI 返回不是 JSON 对象");
        return (ObjectNode) root;
    }

    private Map<String, Object> normalize(ObjectNode source, String inputJson) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", text(source, "summary", "总结", "conclusion"));
        result.put("drivers", list(source, "drivers", "支持证据", "evidence"));
        result.put("counterEvidence", list(source, "counterEvidence", "counter_evidence", "反证"));
        result.put("failureConditions", list(source, "failureConditions", "failure_conditions", "失效条件"));
        result.put("uncertainties", list(source, "uncertainties", "不确定性"));
        result.put("evidenceRefs", list(source, "evidenceRefs", "evidence_refs", "证据引用"));
        result.put("disclaimer", "仅基于已提供的确定性复盘证据，不构成交易建议。");
        result.put("inputBytes", inputJson.length());
        return result;
    }

    private String text(ObjectNode source, String... names) {
        for (String name : names) {
            JsonNode value = source.get(name);
            if (value != null && value.isValueNode() && !value.asText("").isBlank()) return value.asText();
        }
        return "AI 未提供该字段，请以确定性证据为准。";
    }

    private List<String> list(ObjectNode source, String... names) {
        for (String name : names) {
            JsonNode value = source.get(name);
            if (value == null || value.isNull()) continue;
            List<String> result = new ArrayList<>();
            if (value.isArray()) value.forEach(item -> { if (item.isValueNode()) result.add(item.asText()); });
            else if (value.isValueNode()) result.add(value.asText());
            return result;
        }
        return List.of();
    }

    private void record(String executionNo, PromptRuntime prompt, AiModelBO model, String input, AiCompletionResultBO completion, Instant started, Exception error) {
        try {
            executionLogRepository.create(AiExecutionLogBO.builder().executionNo(executionNo).capability(CAPABILITY)
                    .promptKey(prompt.key()).promptVersion(prompt.version()).providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode()).status(error == null ? "SUCCEEDED" : "FAILED")
                    .inputTokens(completion == null ? 0 : completion.getInputTokens()).outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? 0 : completion.getTotalTokens()).latencyMs(Duration.between(started, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("evidencePreview", truncate(input, 4000))))
                    .responseJson(toJson(Map.of("contentPreview", truncate(completion == null ? "" : completion.getContent(), 4000))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000)).extra("{\"rawResponseStored\":false}").build());
        } catch (Exception ignored) { }
    }

    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ignored) { return "{}"; } }
    private String truncate(String value, int limit) { return value == null ? "" : value.length() <= limit ? value : value.substring(0, limit); }
    private record PromptRuntime(String key, String version, String system, String user,
                                 AiPromptRuntimeResolver.AiPromptRuntime runtime) { }
}
