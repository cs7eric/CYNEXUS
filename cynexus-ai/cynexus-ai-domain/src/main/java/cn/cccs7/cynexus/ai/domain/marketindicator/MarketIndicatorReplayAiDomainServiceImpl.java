package cn.cccs7.cynexus.ai.domain.marketindicator;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
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

public final class MarketIndicatorReplayAiDomainServiceImpl implements MarketIndicatorReplayAiDomainService {
    private static final String FALLBACK_VERSION = "v1";
    private static final String FALLBACK_SYSTEM = "你是 Cynexus A 股盘后复盘解释器。只能依据输入证据解释板块 Episode，不得补充外部事实，不得改写评分、时间、收益或数据质量，不得给出买入卖出、目标价、仓位或收益承诺。必须明确支持证据、反证与不确定性。只输出简体中文 JSON。";
    private static final String FALLBACK_USER = "请解释以下确定性复盘证据：{{evidence}}";

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiPromptRepository promptRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final MarketIndicatorReplayAiCompletionPort completionPort;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public MarketIndicatorReplayAiDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                                     AiPromptRepository promptRepository,
                                                     AiExecutionLogRepository executionLogRepository,
                                                     MarketIndicatorReplayAiCompletionPort completionPort) {
        this.routingService = routingService;
        this.promptRepository = promptRepository;
        this.executionLogRepository = executionLogRepository;
        this.completionPort = completionPort;
    }

    @Override
    public Map<String, Object> explain(Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) throw new IllegalArgumentException("复盘证据不能为空");
        String inputJson = toJson(limitEvidence(evidence));
        if (inputJson.length() > 180_000) throw new IllegalArgumentException("复盘证据过大，请先选择单个 Episode");
        AiUsageBindingBO binding = routingService.findBinding(CAPABILITY);
        PromptRuntime prompt = resolvePrompt(binding);
        List<AiModelBO> models = routingService.routeModels(CAPABILITY, null);
        if (models == null || models.isEmpty()) throw new IllegalStateException("未配置可用的复盘解释 AI provider");
        Exception last = null;
        for (AiModelBO model : models) {
            Instant started = Instant.now();
            String executionNo = "MIR-" + UUID.randomUUID().toString().replace("-", "");
            try {
                AiCompletionResultBO completion = completionPort.complete(model, render(prompt, inputJson), CAPABILITY);
                Map<String, Object> normalized = normalize(parse(completion == null ? null : completion.getContent()), inputJson);
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

    private PromptRuntime resolvePrompt(AiUsageBindingBO binding) {
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO prompt = promptRepository.findById(binding.getPromptId());
            if (prompt != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus())) return prompt(prompt);
        }
        List<AiPromptBO> prompts = promptRepository.list(PROMPT_KEY, null, AiConstants.STATUS_ACTIVE);
        return prompts.isEmpty() ? new PromptRuntime(PROMPT_KEY, FALLBACK_VERSION, FALLBACK_SYSTEM, FALLBACK_USER) : prompt(prompts.getFirst());
    }

    private PromptRuntime prompt(AiPromptBO prompt) {
        return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(), defaultString(prompt.getSystemPrompt(), FALLBACK_SYSTEM), defaultString(prompt.getUserPromptTemplate(), FALLBACK_USER));
    }

    private String render(PromptRuntime prompt, String evidence) {
        return prompt.system() + System.lineSeparator() + prompt.user().replace("{{evidence}}", evidence);
    }

    private ObjectNode parse(String content) throws Exception {
        if (content == null || content.isBlank()) throw new IllegalStateException("AI 返回为空");
        String value = content.trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalStateException("AI 未返回 JSON 对象");
        JsonNode root = objectMapper.readTree(value.substring(start, end + 1));
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
                    .requestJson(toJson(Map.of("evidencePreview", truncate(input, AiConstants.MAX_LOG_TEXT_LENGTH))))
                    .responseJson(toJson(Map.of("contentPreview", truncate(completion == null ? "" : completion.getContent(), AiConstants.MAX_LOG_TEXT_LENGTH))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000)).extra("{\"rawResponseStored\":false}").build());
        } catch (Exception ignored) { }
    }

    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ignored) { return "{}"; } }
    private String truncate(String value, int limit) { return value == null ? "" : value.length() <= limit ? value : value.substring(0, limit); }
    private String defaultString(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private record PromptRuntime(String key, String version, String system, String user) { }
}
