package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionAskDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionMetadataDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionTextDTO;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionAskRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionMetadataRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionPolishRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionSummaryRequest;
import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionContentDomainPolicy;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionAiAppService {

    private static final int MAX_LOG_TEXT_LENGTH = 8000;

    private final ApplicationAnalysisRoutingDomainService routingDomainService;
    private final AgentExecutor agentExecutor;
    private final AiPromptRepository aiPromptRepository;
    private final AiExecutionLogRepository aiExecutionLogRepository;
    private final StructuredJsonExtractor structuredJsonExtractor;
    private final ObjectMapper objectMapper;
    private final MediaTranscriptionContentDomainPolicy contentPolicy = new MediaTranscriptionContentDomainPolicy();

    public MediaTranscriptionAiAppService(ApplicationAnalysisRoutingDomainService routingDomainService,
                                          AgentExecutor agentExecutor,
                                          AiPromptRepository aiPromptRepository,
                                          AiExecutionLogRepository aiExecutionLogRepository,
                                          StructuredJsonExtractor structuredJsonExtractor) {
        this.routingDomainService = routingDomainService;
        this.agentExecutor = agentExecutor;
        this.aiPromptRepository = aiPromptRepository;
        this.aiExecutionLogRepository = aiExecutionLogRepository;
        this.structuredJsonExtractor = structuredJsonExtractor;
        this.objectMapper = new ObjectMapper();
    }

    public MediaTranscriptionTextDTO polish(MediaTranscriptionPolishRequest request) {
        String text = contentPolicy.truncate(request == null ? null : request.getText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (text.isBlank()) {
            return MediaTranscriptionTextDTO.builder().text("").build();
        }
        return MediaTranscriptionTextDTO.builder()
                .text(executeText("text_polish", "mp4_text_polish", Map.of("text", text),
                        request == null ? null : request.getPreferredModelCode()))
                .build();
    }

    public MediaTranscriptionTextDTO summary(MediaTranscriptionSummaryRequest request) {
        String text = contentPolicy.truncate(request == null ? null : request.getText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (text.isBlank()) {
            return MediaTranscriptionTextDTO.builder().text("").build();
        }
        String content = executeText("text_summary", "mp4_text_summary", Map.of(
                        "text", text,
                        "title", contentPolicy.normalize(request == null ? null : request.getTitle()),
                        "platform", contentPolicy.normalize(request == null ? null : request.getPlatform()),
                        "duration", request == null || request.getDuration() == null ? "" : String.valueOf(request.getDuration())
                ),
                request == null ? null : request.getPreferredModelCode());
        return MediaTranscriptionTextDTO.builder().text(content).build();
    }

    public MediaTranscriptionMetadataDTO metadata(MediaTranscriptionMetadataRequest request) {
        String polishedText = contentPolicy.truncate(request == null ? null : request.getPolishedText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String summaryText = contentPolicy.truncate(request == null ? null : request.getSummaryText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        if (polishedText.isBlank() && summaryText.isBlank()) {
            MediaTranscriptionContentDomainPolicy.MetadataDraft metadata = contentPolicy.fallbackMetadata();
            return MediaTranscriptionMetadataDTO.builder().title(metadata.title()).categories(metadata.categories()).tags(metadata.tags()).build();
        }
        String content = executeText("post_metadata", "post_metadata", Map.of(
                        "polished_text", polishedText,
                        "summary_text", summaryText
                ),
                request == null ? null : request.getPreferredModelCode());
        MediaTranscriptionContentDomainPolicy.MetadataDraft metadata = contentPolicy.parseMetadata(
                structuredJsonExtractor.extractObject(content));
        return MediaTranscriptionMetadataDTO.builder().title(metadata.title()).categories(metadata.categories()).tags(metadata.tags()).build();
    }

    public MediaTranscriptionAskDTO ask(MediaTranscriptionAskRequest request) {
        String polishedText = contentPolicy.truncate(request == null ? null : request.getPolishedText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String summaryText = contentPolicy.truncate(request == null ? null : request.getSummaryText(), MediaTranscriptionContentDomainPolicy.MAX_INPUT_LENGTH);
        String question = contentPolicy.normalize(request == null ? null : request.getQuestion());
        if (question.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "question is required");
        }
        if (polishedText.isBlank() && summaryText.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "content is required");
        }
        String content = executeText("mp4_qa", "mp4_qa", Map.of(
                        "polished_text", polishedText,
                        "summary_text", summaryText,
                        "question", question
                ),
                request == null ? null : request.getPreferredModelCode());
        return MediaTranscriptionAskDTO.builder().answer(content).build();
    }

    private String executeText(String capability, String promptKey, Map<String, String> variables, String preferredModelCode) {
        PromptRuntime prompt = resolvePrompt(promptKey, capability);
        AiScenarioBO scenario = buildScenario(capability, jsonRequired(capability));
        List<AiModelBO> models = routeModels(scenario, capability, preferredModelCode);
        String fullPrompt = render(prompt, variables);
        Exception lastException = null;
        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = agentExecutor.completeDetailed(model, fullPrompt, capability);
                recordExecutionLog(executionNo, capability, prompt, model, "SUCCEEDED", startedAt, fullPrompt, completion, null);
                return normalize(completion.getContent());
            } catch (Exception exception) {
                lastException = exception;
                recordExecutionLog(executionNo, capability, prompt, model, "FAILED", startedAt, fullPrompt, null, exception);
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR,
                "AI " + capability + " failed: " + (lastException == null ? "no available model" : lastException.getMessage()));
    }

    private PromptRuntime resolvePrompt(String promptKey, String capability) {
        List<AiPromptBO> prompts = aiPromptRepository.list(promptKey, null, "active").stream()
                .filter(prompt -> promptKey.equals(prompt.getPromptKey()))
                .toList();
        if (!prompts.isEmpty()) {
            AiPromptBO prompt = prompts.get(0);
            return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(),
                    normalize(prompt.getSystemPrompt()), normalize(prompt.getUserPromptTemplate()));
        }
        return defaultPrompt(promptKey, capability);
    }

    private PromptRuntime defaultPrompt(String promptKey, String capability) {
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

    private List<AiModelBO> routeModels(AiScenarioBO scenario, String capability, String preferredModelCode) {
        return routingDomainService.routeModels(capability, preferredModelCode);
    }

    private String render(PromptRuntime prompt, Map<String, String> variables) {
        String user = prompt.userPromptTemplate();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            user = user.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return prompt.systemPrompt() + System.lineSeparator() + user;
    }

    private AiScenarioBO buildScenario(String capability, boolean jsonRequired) {
        return AiScenarioBO.builder()
                .scenarioCode(capability)
                .scenarioName("Media Transcription " + capability)
                .defaultModelCode("mock-model")
                .maxToolCalls(0)
                .toolCallRequired(false)
                .jsonOutputRequired(jsonRequired)
                .enabled(true)
                .build();
    }

    private boolean jsonRequired(String capability) {
        return "mp4_qa".equals(capability) || "post_metadata".equals(capability);
    }

    private void recordExecutionLog(String executionNo, String capability, PromptRuntime prompt, AiModelBO model,
                                    String status, Instant startedAt, String requestText,
                                    AiCompletionResultBO completion, Exception error) {
        try {
            aiExecutionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo(executionNo)
                    .capability(capability)
                    .promptKey(prompt.promptKey())
                    .promptVersion(prompt.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status(status)
                    .inputTokens(completion == null ? estimateTokens(requestText) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? estimateTokens(requestText) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of("promptPreview", truncate(requestText, MAX_LOG_TEXT_LENGTH))))
                    .responseJson(toJson(Map.of("contentPreview", truncate(completion == null ? "" : completion.getContent(), MAX_LOG_TEXT_LENGTH))))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000))
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

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private record PromptRuntime(String promptKey, String promptVersion, String systemPrompt, String userPromptTemplate) {
    }
}
