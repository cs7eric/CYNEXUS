package cn.cccs7.cynexus.ai.domain.mediatranscription.impl;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionEnrichmentBO;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionEnrichmentCommand;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionContentDomainPolicy;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import cn.cccs7.cynexus.ai.domain.validation.BoundedStructuredOutputRepairPolicy;

public class MediaTranscriptionDomainServiceImpl implements MediaTranscriptionDomainService {

    private static final String ENRICH_CAPABILITY = "media_transcription_enrich";
    private static final String ENRICH_PROMPT_KEY = "media_transcription_enrich";
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("复盘", "交易技巧", "投资方法", "市场分析", "行业研究", "公司研究", "宏观政策", "学习笔记", "其他");

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final AiModelExecutor modelExecutor;
    private final AiPromptRuntimeResolver promptResolver;
    private final AiExecutionLogRepository executionLogRepository;
    private final MediaTranscriptionStructuredOutputPort structuredOutputPort;
    private final MediaTranscriptionContentDomainPolicy contentPolicy = new MediaTranscriptionContentDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaTranscriptionDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                              AiModelExecutor modelExecutor,
                                              AiPromptRuntimeResolver promptResolver,
                                              AiExecutionLogRepository executionLogRepository,
                                              MediaTranscriptionStructuredOutputPort structuredOutputPort) {
        this.routingService = routingService;
        this.modelExecutor = modelExecutor;
        this.promptResolver = promptResolver;
        this.executionLogRepository = executionLogRepository;
        this.structuredOutputPort = structuredOutputPort;
    }

    @Override
    public MediaTranscriptionEnrichmentBO enrich(MediaTranscriptionEnrichmentCommand command) {
        String transcript = contentPolicy.truncate(command == null ? null : command.transcript(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (transcript.isBlank()) return new MediaTranscriptionEnrichmentBO("", "", "", List.of(), List.of(), List.of(), List.of(), List.of(), "", List.of(), List.of());
        Map<String, String> variables = Map.of(
                "transcript", transcript,
                "title", contentPolicy.normalize(command == null ? null : command.title()),
                "platform", contentPolicy.normalize(command == null ? null : command.platform()),
                "duration", command == null || command.duration() == null ? "" : String.valueOf(command.duration()));
        String content = executeText(ENRICH_CAPABILITY, ENRICH_PROMPT_KEY, variables,
                command == null ? null : command.preferredModelCode(), command == null ? null : command.tenantKey(),
                command == null ? null : command.principalKey());
        try {
            var repaired = BoundedStructuredOutputRepairPolicy.parseOrRepair(content, null, structuredOutputPort,
                    256_000, raw -> executeText(ENRICH_CAPABILITY, "media_transcription_enrich_repair",
                            Map.of("transcript", transcript, "title", variables.get("title"), "platform", variables.get("platform"),
                                    "duration", variables.get("duration"), "raw_output", raw, "error", "invalid structured enrichment"),
                            command == null ? null : command.preferredModelCode(), command == null ? null : command.tenantKey(),
                            command == null ? null : command.principalKey()));
            return parseEnrichment(repaired.json());
        } catch (BoundedStructuredOutputRepairPolicy.RepairFailedException failure) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, failure.getMessage());
        }
    }

    @Override
    public String polish(TextCommand command) {
        String text = contentPolicy.truncate(command == null ? null : command.text(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        return text.isBlank() ? "" : executeText("text_polish", "mp4_text_polish", Map.of("text", text), model(command));
    }

    @Override
    public String summary(SummaryCommand command) {
        String text = contentPolicy.truncate(command == null ? null : command.text(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (text.isBlank()) return "";
        return executeText("text_summary", "mp4_text_summary", Map.of("text", text,
                "title", contentPolicy.normalize(command == null ? null : command.title()),
                "platform", contentPolicy.normalize(command == null ? null : command.platform()),
                "duration", command == null || command.duration() == null ? "" : String.valueOf(command.duration())), model(command));
    }

    @Override
    public MediaTranscriptionMetadataBO metadata(MetadataCommand command) {
        String polished = contentPolicy.truncate(command == null ? null : command.polishedText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String summary = contentPolicy.truncate(command == null ? null : command.summaryText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (polished.isBlank() && summary.isBlank()) return toMetadata(contentPolicy.fallbackMetadata());
        String content = executeText("post_metadata", "post_metadata", Map.of("polished_text", polished, "summary_text", summary), model(command));
        return toMetadata(contentPolicy.parseMetadata(structuredOutputPort.extractObject(content)));
    }

    @Override
    public String ask(AskCommand command) {
        String polished = contentPolicy.truncate(command == null ? null : command.polishedText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String summary = contentPolicy.truncate(command == null ? null : command.summaryText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String question = contentPolicy.normalize(command == null ? null : command.question());
        if (question.isBlank()) throw new BizException(ErrorCode.BAD_REQUEST, "question is required");
        if (polished.isBlank() && summary.isBlank()) throw new BizException(ErrorCode.BAD_REQUEST, "content is required");
        return executeText("mp4_qa", "mp4_qa", Map.of("polished_text", polished, "summary_text", summary, "question", question), model(command));
    }

    private String executeText(String capability, String promptKey, Map<String, String> variables, String preferredModelCode) {
        return executeText(capability, promptKey, variables, preferredModelCode, null, null);
    }

    private String executeText(String capability, String promptKey, Map<String, String> variables, String preferredModelCode,
                               String tenantKey, String principalKey) {
        AiPromptRuntimeResolver.AiPromptRuntime prompt = resolvePrompt(promptKey, capability);
        AiScenarioBO scenario = AiScenarioBO.builder().scenarioCode(capability).scenarioName("Media Transcription " + capability)
                .maxToolCalls(0).toolCallRequired(false).jsonOutputRequired(jsonRequired(capability)).enabled(true).build();
        List<AiModelBO> models = routingService.routeModels(capability, preferredModelCode);
        if (models == null || models.isEmpty()) throw new BizException(ErrorCode.INTERNAL_ERROR, "AI " + capability + " failed: no available model");
        String fullPrompt = prompt.render(variables);
        Exception lastException = null;
        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = modelExecutor.complete(model, fullPrompt,
                        new AiExecutionContext(executionNo, capability, prompt.promptKey(), prompt.promptVersion(),
                                model.getProviderCode(), model.getModelCode(), tenantKey, principalKey, executionNo,
                                cn.cccs7.cynexus.common.core.constant.TraceId.current()));
                recordExecutionLog(executionNo, capability, prompt, model, "SUCCEEDED", startedAt, fullPrompt, completion, null);
                return normalize(completion.getContent());
            } catch (Exception exception) {
                lastException = exception;
                recordExecutionLog(executionNo, capability, prompt, model, "FAILED", startedAt, fullPrompt, null, exception);
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "AI " + capability + " failed: " + (lastException == null ? "no available model" : lastException.getMessage()));
    }

    private MediaTranscriptionEnrichmentBO parseEnrichment(String content) {
        try {
            String object = structuredOutputPort.extractObject(content);
            var root = objectMapper.readTree(object);
            return new MediaTranscriptionEnrichmentBO(
                    bounded(root.path("polishedText").asText(""), 120_000),
                    bounded(root.path("summary").asText(""), 30_000),
                    bounded(root.path("shortSummary").asText(""), 2_000),
                    strings(root.path("keyPoints"), 12, 500), strings(root.path("topics"), 12, 120),
                    strings(root.path("methodology"), 12, 1_000), strings(root.path("actionItems"), 12, 500),
                    strings(root.path("risks"), 12, 500), bounded(root.path("title").asText(""), 80),
                    categories(root.path("categories")), strings(root.path("tags"), 20, 80));
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "AI media enrichment returned invalid JSON");
        }
    }

    private List<String> strings(com.fasterxml.jackson.databind.JsonNode node, int maxItems, int maxLength) {
        if (!node.isArray()) return List.of();
        List<String> values = new java.util.ArrayList<>();
        node.forEach(item -> { if (values.size() < maxItems && item.isTextual() && !item.asText().isBlank()) values.add(bounded(item.asText(), maxLength)); });
        return Collections.unmodifiableList(values);
    }

    private List<String> categories(com.fasterxml.jackson.databind.JsonNode node) {
        return strings(node, 8, 80).stream().filter(ALLOWED_CATEGORIES::contains).distinct().toList();
    }

    private String bounded(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private AiPromptRuntimeResolver.AiPromptRuntime resolvePrompt(String promptKey, String capability) {
        String system = switch (promptKey) {
            case "mp4_text_polish" -> "你是中文语音转写文本校对助手。保持原意，纠错、断句、分段，直接输出润色后的纯文本。";
            case "mp4_text_summary" -> "你是股票投资知识整理助手。基于输入文本提炼核心主题、核心观点、关键知识点、方法论、风险提醒，输出结构化纯文本。";
            case "post_metadata" -> "为股票投资知识分享文章生成 JSON：{\"title\":\"\",\"categories\":[],\"tags\":[]}。只输出 JSON。";
            case "mp4_qa" -> "基于原文和摘要回答问题，只输出合法 qa_v1 JSON。";
            case "media_transcription_enrich" -> "你是中文音视频知识整理助手。只能依据 transcript 内容，先纠错润色，再生成摘要、关键观点、方法论、行动清单、风险提醒以及知识库分类。不得补充外部事实、投资建议或不存在的观点。只输出合法 JSON。";
            case "media_transcription_enrich_repair" -> "修复音视频知识整理结果。只能输出合法 JSON，字段必须为 polishedText、summary、shortSummary、keyPoints、topics、methodology、actionItems、risks、title、categories、tags。不得补充 transcript 之外的事实。";
            default -> "";
        };
        String user = switch (promptKey) {
            case "post_metadata" -> "润色文本：\n{{polished_text}}\n\n摘要：\n{{summary_text}}";
            case "mp4_qa" -> "【原文内容（润色后）】\n{{polished_text}}\n\n【AI 摘要总结】\n{{summary_text}}\n\n【用户问题】\n{{question}}";
            case "media_transcription_enrich" -> "输入标题：{{title}}\n平台：{{platform}}\n时长：{{duration}}\n\nTranscript：\n{{transcript}}\n\n输出字段必须包含 polishedText、summary、shortSummary、keyPoints、topics、methodology、actionItems、risks、title、categories、tags。title 必须是概括主题的短标题，控制在 30 个中文字符以内，不得复述正文；categories 只使用复盘、交易技巧、投资方法、市场分析、行业研究、公司研究、宏观政策、学习笔记、其他；tags 为简短中文标签。";
            case "media_transcription_enrich_repair" -> "Transcript：\n{{transcript}}\n\n原始模型输出：\n{{raw_output}}\n\n解析错误：{{error}}\n\n请仅返回修复后的 JSON。";
            default -> "{{text}}";
        };
        return promptResolver.resolve(capability, promptKey, system, user);
    }

    private void recordExecutionLog(String executionNo, String capability, AiPromptRuntimeResolver.AiPromptRuntime prompt, AiModelBO model, String status,
                                    Instant startedAt, String requestText, AiCompletionResultBO completion, Exception error) {
        try {
            executionLogRepository.create(AiExecutionLogBO.builder().executionNo(executionNo).capability(capability)
                    .promptKey(prompt.promptKey()).promptVersion(prompt.promptVersion()).providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode()).status(status)
                    .inputTokens(completion == null ? estimateTokens(requestText) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? estimateTokens(requestText) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("promptPreview", truncate(requestText, AiConstants.MAX_LOG_TEXT_LENGTH))))
                    .responseJson(toJson(Map.of("contentPreview", truncate(completion == null ? "" : completion.getContent(), AiConstants.MAX_LOG_TEXT_LENGTH))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000)).extra("{}").build());
        } catch (Exception ignored) { }
    }

    private MediaTranscriptionMetadataBO toMetadata(MediaTranscriptionContentDomainPolicy.MetadataDraft metadata) {
        return new MediaTranscriptionMetadataBO(metadata.title(), metadata.categories(), metadata.tags());
    }

    private String model(TextCommand command) { return command == null ? null : command.preferredModelCode(); }
    private String model(SummaryCommand command) { return command == null ? null : command.preferredModelCode(); }
    private String model(MetadataCommand command) { return command == null ? null : command.preferredModelCode(); }
    private String model(AskCommand command) { return command == null ? null : command.preferredModelCode(); }
    private boolean jsonRequired(String capability) { return "mp4_qa".equals(capability) || "post_metadata".equals(capability) || ENRICH_CAPABILITY.equals(capability); }
    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String truncate(String value, int maxLength) { String normalized = normalize(value); return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized; }
    private int estimateTokens(String value) { return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4); }
    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ignored) { return "{}"; } }

}
