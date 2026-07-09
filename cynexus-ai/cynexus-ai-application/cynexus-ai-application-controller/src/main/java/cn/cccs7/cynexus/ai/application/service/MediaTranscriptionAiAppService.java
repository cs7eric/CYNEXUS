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
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionAiAppService {

    private static final int MAX_INPUT_LENGTH = 60000;
    private static final int MAX_LOG_TEXT_LENGTH = 8000;

    private final ModelRouteAppService modelRouteAppService;
    private final AgentExecutor agentExecutor;
    private final AiPromptRepository aiPromptRepository;
    private final AiUsageBindingRepository aiUsageBindingRepository;
    private final AiProviderRepository aiProviderRepository;
    private final AiExecutionLogRepository aiExecutionLogRepository;
    private final StructuredJsonExtractor structuredJsonExtractor;
    private final ObjectMapper objectMapper;

    public MediaTranscriptionAiAppService(ModelRouteAppService modelRouteAppService,
                                          AgentExecutor agentExecutor,
                                          AiPromptRepository aiPromptRepository,
                                          AiUsageBindingRepository aiUsageBindingRepository,
                                          AiProviderRepository aiProviderRepository,
                                          AiExecutionLogRepository aiExecutionLogRepository,
                                          StructuredJsonExtractor structuredJsonExtractor) {
        this.modelRouteAppService = modelRouteAppService;
        this.agentExecutor = agentExecutor;
        this.aiPromptRepository = aiPromptRepository;
        this.aiUsageBindingRepository = aiUsageBindingRepository;
        this.aiProviderRepository = aiProviderRepository;
        this.aiExecutionLogRepository = aiExecutionLogRepository;
        this.structuredJsonExtractor = structuredJsonExtractor;
        this.objectMapper = new ObjectMapper();
    }

    public MediaTranscriptionTextDTO polish(MediaTranscriptionPolishRequest request) {
        String text = truncate(normalize(request == null ? null : request.getText()), MAX_INPUT_LENGTH);
        if (text.isBlank()) {
            return MediaTranscriptionTextDTO.builder().text("").build();
        }
        return MediaTranscriptionTextDTO.builder()
                .text(executeText("text_polish", "mp4_text_polish", Map.of("text", text),
                        request == null ? null : request.getPreferredModelCode()))
                .build();
    }

    public MediaTranscriptionTextDTO summary(MediaTranscriptionSummaryRequest request) {
        String text = truncate(normalize(request == null ? null : request.getText()), MAX_INPUT_LENGTH);
        if (text.isBlank()) {
            return MediaTranscriptionTextDTO.builder().text("").build();
        }
        String content = executeText("text_summary", "mp4_text_summary", Map.of(
                        "text", text,
                        "title", normalize(request == null ? null : request.getTitle()),
                        "platform", normalize(request == null ? null : request.getPlatform()),
                        "duration", request == null || request.getDuration() == null ? "" : String.valueOf(request.getDuration())
                ),
                request == null ? null : request.getPreferredModelCode());
        return MediaTranscriptionTextDTO.builder().text(content).build();
    }

    public MediaTranscriptionMetadataDTO metadata(MediaTranscriptionMetadataRequest request) {
        String polishedText = truncate(normalize(request == null ? null : request.getPolishedText()), MAX_INPUT_LENGTH);
        String summaryText = truncate(normalize(request == null ? null : request.getSummaryText()), MAX_INPUT_LENGTH);
        if (polishedText.isBlank() && summaryText.isBlank()) {
            return fallbackMetadata();
        }
        String content = executeText("post_metadata", "post_metadata", Map.of(
                        "polished_text", polishedText,
                        "summary_text", summaryText
                ),
                request == null ? null : request.getPreferredModelCode());
        return parseMetadata(content);
    }

    public MediaTranscriptionAskDTO ask(MediaTranscriptionAskRequest request) {
        String polishedText = truncate(normalize(request == null ? null : request.getPolishedText()), MAX_INPUT_LENGTH);
        String summaryText = truncate(normalize(request == null ? null : request.getSummaryText()), MAX_INPUT_LENGTH);
        String question = normalize(request == null ? null : request.getQuestion());
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
                AiCompletionResultBO completion = agentExecutor.completeDetailed(model, fullPrompt);
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
        List<AiModelBO> bindingModels = routeByUsageBinding(capability, preferredModelCode);
        if (!bindingModels.isEmpty()) {
            return bindingModels;
        }
        try {
            return List.of(modelRouteAppService.route(scenario, preferredModelCode));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<AiModelBO> routeByUsageBinding(String capability, String preferredModelCode) {
        AiUsageBindingBO binding = aiUsageBindingRepository.listAll().stream()
                .filter(item -> capability.equals(item.getCapability()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .findFirst()
                .orElse(null);
        List<AiModelBO> models = new ArrayList<>();
        Set<String> modelKeys = new LinkedHashSet<>();
        Long bindingProviderId = null;
        if (binding != null && binding.getProviderId() != null) {
            bindingProviderId = binding.getProviderId();
            AiProviderBO provider = aiProviderRepository.findById(binding.getProviderId());
            addProviderModels(models, modelKeys, provider, List.of(
                    normalize(preferredModelCode),
                    normalize(binding.getModelOverride()),
                    provider == null ? "" : normalize(provider.getDefaultModel())
            ));
        }

        for (AiProviderBO provider : aiProviderRepository.listAll()) {
            if (provider == null || provider.getId() == null || provider.getId().equals(bindingProviderId)) {
                continue;
            }
            addProviderModels(models, modelKeys, provider, List.of(normalize(provider.getDefaultModel())));
        }
        return models;
    }

    private void addProviderModels(List<AiModelBO> models, Set<String> modelKeys, AiProviderBO provider, List<String> modelNames) {
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            return;
        }
        String providerCode = normalize(provider.getProviderType()).isBlank()
                ? normalize(provider.getCode())
                : normalize(provider.getProviderType());
        if (providerCode.isBlank() || "mock".equalsIgnoreCase(providerCode)) {
            return;
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            addIfPresent(names, modelName);
        }
        for (String modelName : names) {
            String key = providerCode + "|" + modelName;
            if (!modelKeys.add(key)) {
                continue;
            }
            models.add(AiModelBO.builder()
                    .modelCode(modelName)
                    .providerCode(providerCode)
                    .modelName(modelName)
                    .baseUrl(provider.getBaseUrl())
                    .apiKey(provider.getApiKey())
                    .apiKeyEnv(provider.getApiKeyEnv())
                    .extra(provider.getExtra())
                    .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                    .enabled(true)
                    .build());
        }
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

    private MediaTranscriptionMetadataDTO parseMetadata(String content) {
        try {
            JsonNode root = objectMapper.readTree(structuredJsonExtractor.extractObject(content));
            return MediaTranscriptionMetadataDTO.builder()
                    .title(blankTo(root.path("title").asText(null), "未命名笔记"))
                    .categories(readStringList(root.get("categories"), List.of("未分类")))
                    .tags(readStringList(root.get("tags"), List.of("待整理")))
                    .build();
        } catch (Exception ignored) {
            return fallbackMetadata();
        }
    }

    private MediaTranscriptionMetadataDTO fallbackMetadata() {
        return MediaTranscriptionMetadataDTO.builder()
                .title("未命名笔记")
                .categories(List.of("未分类"))
                .tags(List.of("待整理"))
                .build();
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = normalize(item.asText(""));
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? fallback : values;
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

    private void addIfPresent(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
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

    private String blankTo(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private record PromptRuntime(String promptKey, String promptVersion, String systemPrompt, String userPromptTemplate) {
    }
}
