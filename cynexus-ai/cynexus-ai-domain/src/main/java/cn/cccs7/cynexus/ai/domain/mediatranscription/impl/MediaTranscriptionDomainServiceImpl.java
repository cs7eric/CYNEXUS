package cn.cccs7.cynexus.ai.domain.mediatranscription.impl;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionCompletionPort;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionContentDomainPolicy;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MediaTranscriptionDomainServiceImpl implements MediaTranscriptionDomainService {

    private final ApplicationAnalysisRoutingDomainService routingService;
    private final MediaTranscriptionCompletionPort completionPort;
    private final AiPromptRepository promptRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final MediaTranscriptionStructuredOutputPort structuredOutputPort;
    private final MediaTranscriptionContentDomainPolicy contentPolicy = new MediaTranscriptionContentDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaTranscriptionDomainServiceImpl(ApplicationAnalysisRoutingDomainService routingService,
                                              MediaTranscriptionCompletionPort completionPort,
                                              AiPromptRepository promptRepository,
                                              AiExecutionLogRepository executionLogRepository,
                                              MediaTranscriptionStructuredOutputPort structuredOutputPort) {
        this.routingService = routingService;
        this.completionPort = completionPort;
        this.promptRepository = promptRepository;
        this.executionLogRepository = executionLogRepository;
        this.structuredOutputPort = structuredOutputPort;
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
        PromptRuntime prompt = resolvePrompt(promptKey, capability);
        AiScenarioBO scenario = AiScenarioBO.builder().scenarioCode(capability).scenarioName("Media Transcription " + capability)
                .maxToolCalls(0).toolCallRequired(false).jsonOutputRequired(jsonRequired(capability)).enabled(true).build();
        List<AiModelBO> models = routingService.routeModels(capability, preferredModelCode);
        if (models == null || models.isEmpty()) throw new BizException(ErrorCode.INTERNAL_ERROR, "AI " + capability + " failed: no available model");
        String fullPrompt = render(prompt, variables);
        Exception lastException = null;
        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = completionPort.complete(model, fullPrompt, capability);
                recordExecutionLog(executionNo, capability, prompt, model, "SUCCEEDED", startedAt, fullPrompt, completion, null);
                return normalize(completion.getContent());
            } catch (Exception exception) {
                lastException = exception;
                recordExecutionLog(executionNo, capability, prompt, model, "FAILED", startedAt, fullPrompt, null, exception);
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "AI " + capability + " failed: " + (lastException == null ? "no available model" : lastException.getMessage()));
    }

    private PromptRuntime resolvePrompt(String promptKey, String capability) {
        AiUsageBindingBO binding = routingService.findBinding(capability);
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO bound = promptRepository.findById(binding.getPromptId());
            if (bound != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(bound.getStatus())) return prompt(bound);
        }
        List<AiPromptBO> prompts = promptRepository.list(promptKey, null, AiConstants.STATUS_ACTIVE).stream()
                .filter(prompt -> promptKey.equals(prompt.getPromptKey())).toList();
        return prompts.isEmpty() ? defaultPrompt(promptKey) : prompt(prompts.getFirst());
    }

    private PromptRuntime prompt(AiPromptBO prompt) {
        return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(), normalize(prompt.getSystemPrompt()), normalize(prompt.getUserPromptTemplate()));
    }

    private PromptRuntime defaultPrompt(String promptKey) {
        String system = switch (promptKey) {
            case "mp4_text_polish" -> "你是中文语音转写文本校对助手。保持原意，纠错、断句、分段，直接输出润色后的纯文本。";
            case "mp4_text_summary" -> "你是股票投资知识整理助手。基于输入文本提炼核心主题、核心观点、关键知识点、方法论、风险提醒，输出结构化纯文本。";
            case "post_metadata" -> "为股票投资知识分享文章生成 JSON：{\"title\":\"\",\"categories\":[],\"tags\":[]}。只输出 JSON。";
            case "mp4_qa" -> "基于原文和摘要回答问题，只输出合法 qa_v1 JSON。";
            default -> "";
        };
        String user = switch (promptKey) {
            case "post_metadata" -> "润色文本：\n{{polished_text}}\n\n摘要：\n{{summary_text}}";
            case "mp4_qa" -> "【原文内容（润色后）】\n{{polished_text}}\n\n【AI 摘要总结】\n{{summary_text}}\n\n【用户问题】\n{{question}}";
            default -> "{{text}}";
        };
        return new PromptRuntime(promptKey, "fallback-service-default", system, user);
    }

    private String render(PromptRuntime prompt, Map<String, String> variables) {
        String user = prompt.userPromptTemplate();
        for (Map.Entry<String, String> entry : variables.entrySet()) user = user.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        return prompt.systemPrompt() + System.lineSeparator() + user;
    }

    private void recordExecutionLog(String executionNo, String capability, PromptRuntime prompt, AiModelBO model, String status,
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
    private boolean jsonRequired(String capability) { return "mp4_qa".equals(capability) || "post_metadata".equals(capability); }
    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String truncate(String value, int maxLength) { String normalized = normalize(value); return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized; }
    private int estimateTokens(String value) { return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4); }
    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ignored) { return "{}"; } }

    private record PromptRuntime(String promptKey, String promptVersion, String systemPrompt, String userPromptTemplate) { }
}
