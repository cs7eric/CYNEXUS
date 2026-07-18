package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseGenerateRequest;
import cn.cccs7.cynexus.ai.application.prompt.KnowledgeBaseStructuredOutputParser;
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
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseDocumentGenerationAppService {

    private static final String CAPABILITY = "knowledge_base_document_generate";
    private static final String PROMPT_KEY = "knowledge_base_document_generate";
    private static final int MAX_SOURCE_LENGTH = 20000;
    private static final int MAX_LOG_TEXT_LENGTH = 8000;
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是 Cynexus 知识库 Markdown 文档整理助手。
            任务：根据用户输入的一段原始文本，提炼出适合保存到知识库的文档草稿。

            要求：
            - title：简洁明确，优先中文，最多 40 个字。
            - description：一句话概括文档用途，最多 120 个字。
            - tags：3 到 8 个标签，使用短词，不要重复。
            - categories：1 到 3 个分类，偏业务主题，不要和 tags 完全重复。
            - content：输出完整 Markdown 正文，结构清晰，保留原文中的关键事实、步骤、列表、代码或表格信息。
            - 不要编造输入中没有的事实。
            - 列表为空时输出 []，字符串为空时输出 ""。
            - 严格输出合法 JSON，不要添加 Markdown 代码块或额外解释。
            """;
    private static final String DEFAULT_USER_PROMPT_TEMPLATE = """
            {{formatInstructions}}

            原始文本：
            {{sourceText}}
            """;

    private final ModelRouteAppService modelRouteAppService;
    private final AgentExecutor agentExecutor;
    private final AiProviderRepository aiProviderRepository;
    private final AiUsageBindingRepository aiUsageBindingRepository;
    private final AiPromptRepository aiPromptRepository;
    private final AiExecutionLogRepository aiExecutionLogRepository;
    private final StructuredJsonExtractor structuredJsonExtractor;
    private final KnowledgeBaseStructuredOutputParser outputParser;
    private final ObjectMapper objectMapper;
    private final KnowledgeBaseDocumentDraftPolicy draftPolicy = new KnowledgeBaseDocumentDraftPolicy();

    public KnowledgeBaseDocumentGenerationAppService(ModelRouteAppService modelRouteAppService,
                                                     AgentExecutor agentExecutor,
                                                     AiProviderRepository aiProviderRepository,
                                                     AiUsageBindingRepository aiUsageBindingRepository,
                                                     AiPromptRepository aiPromptRepository,
                                                     AiExecutionLogRepository aiExecutionLogRepository,
                                                     StructuredJsonExtractor structuredJsonExtractor,
                                                     KnowledgeBaseStructuredOutputParser outputParser) {
        this.modelRouteAppService = modelRouteAppService;
        this.agentExecutor = agentExecutor;
        this.aiProviderRepository = aiProviderRepository;
        this.aiUsageBindingRepository = aiUsageBindingRepository;
        this.aiPromptRepository = aiPromptRepository;
        this.aiExecutionLogRepository = aiExecutionLogRepository;
        this.structuredJsonExtractor = structuredJsonExtractor;
        this.outputParser = outputParser;
        this.objectMapper = new ObjectMapper();
    }

    public KnowledgeBaseGenerateDTO generate(KnowledgeBaseGenerateRequest request) {
        String sourceText = request == null ? "" : draftPolicy.normalizeSource(request.getSourceText());
        if (sourceText.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "sourceText is required");
        }
        sourceText = draftPolicy.truncateSource(sourceText);

        AiUsageBindingBO binding = findUsageBinding(request);
        PromptRuntime promptRuntime = resolvePrompt(binding);
        AiScenarioBO scenario = buildScenario();
        List<AiModelBO> models = routeModels(scenario, binding, request);
        String prompt = buildPrompt(promptRuntime, sourceText);
        Exception lastException = null;

        for (AiModelBO model : models) {
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            Instant startedAt = Instant.now();
            try {
                AiCompletionResultBO completion = agentExecutor.completeDetailed(model, prompt, CAPABILITY);
                KnowledgeBaseGenerateDTO result = parseOrRepair(model, promptRuntime, sourceText, completion);
                recordExecutionLog(executionNo, promptRuntime, model, "SUCCEEDED", startedAt,
                        sourceText, completion, null);
                return normalizeResult(result, sourceText);
            } catch (Exception e) {
                lastException = e;
                recordExecutionLog(executionNo, promptRuntime, model, "FAILED", startedAt,
                        sourceText, null, e);
            }
        }

        return fallbackResult(sourceText, lastException);
    }

    private KnowledgeBaseGenerateDTO parseOrRepair(AiModelBO model,
                                                   PromptRuntime promptRuntime,
                                                   String sourceText,
                                                   AiCompletionResultBO completion) throws Exception {
        try {
            return outputParser.convert(structuredJsonExtractor.extractObject(completion.getContent()));
        } catch (Exception parseException) {
            String repairPrompt = buildRepairPrompt(promptRuntime, sourceText, completion.getContent(), parseException);
            AiCompletionResultBO repaired = agentExecutor.completeDetailed(model, repairPrompt, CAPABILITY);
            return outputParser.convert(structuredJsonExtractor.extractObject(repaired.getContent()));
        }
    }

    private AiScenarioBO buildScenario() {
        return AiScenarioBO.builder()
                .scenarioCode(CAPABILITY)
                .scenarioName("Knowledge Base Document Generation")
                .defaultModelCode("mock-model")
                .maxToolCalls(0)
                .toolCallRequired(false)
                .jsonOutputRequired(true)
                .enabled(true)
                .build();
    }

    private AiUsageBindingBO findUsageBinding(KnowledgeBaseGenerateRequest request) {
        return aiUsageBindingRepository.listAll().stream()
                .filter(item -> CAPABILITY.equals(item.getCapability()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> matchesRoutingRequest(item, request))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesRoutingRequest(AiUsageBindingBO binding, KnowledgeBaseGenerateRequest request) {
        JsonNode params = readJson(binding == null ? null : binding.getParams());
        if (request == null || params.isMissingNode() || params.isNull()) {
            return true;
        }
        return matchesStringParam(params, "tenant_code", request.getTenantCode())
                && matchesStringParam(params, "quality", request.getRoutingQuality())
                && matchesStringParam(params, "cost_tier", request.getMaxCostTier());
    }

    private boolean matchesStringParam(JsonNode params, String field, String requested) {
        String normalized = normalize(requested);
        if (normalized.isBlank()) {
            return true;
        }
        JsonNode value = params.get(field);
        if (value == null || value.isNull()) {
            return true;
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                if (normalized.equalsIgnoreCase(item.asText(""))) {
                    return true;
                }
            }
            return false;
        }
        return normalized.equalsIgnoreCase(value.asText(""));
    }

    private PromptRuntime resolvePrompt(AiUsageBindingBO binding) {
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO boundPrompt = aiPromptRepository.findById(binding.getPromptId());
            if (boundPrompt != null && "active".equalsIgnoreCase(boundPrompt.getStatus())) {
                return promptRuntime(boundPrompt);
            }
        }
        String promptVersion = stringParam(binding == null ? null : binding.getParams(), "prompt_version");
        List<AiPromptBO> prompts = aiPromptRepository.list(PROMPT_KEY, null, "active").stream()
                .filter(prompt -> PROMPT_KEY.equals(prompt.getPromptKey()))
                .filter(prompt -> promptVersion.isBlank() || promptVersion.equals(prompt.getPromptVersion()))
                .toList();
        if (!prompts.isEmpty()) {
            AiPromptBO prompt = prompts.get(0);
            return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(),
                    normalize(prompt.getSystemPrompt()).isBlank() ? DEFAULT_SYSTEM_PROMPT : prompt.getSystemPrompt(),
                    normalize(prompt.getUserPromptTemplate()).isBlank()
                            ? DEFAULT_USER_PROMPT_TEMPLATE
                            : prompt.getUserPromptTemplate());
        }
        return new PromptRuntime(PROMPT_KEY, "fallback-service-default", DEFAULT_SYSTEM_PROMPT,
                DEFAULT_USER_PROMPT_TEMPLATE);
    }

    private PromptRuntime promptRuntime(AiPromptBO prompt) {
        return new PromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(),
                normalize(prompt.getSystemPrompt()).isBlank() ? DEFAULT_SYSTEM_PROMPT : prompt.getSystemPrompt(),
                normalize(prompt.getUserPromptTemplate()).isBlank() ? DEFAULT_USER_PROMPT_TEMPLATE : prompt.getUserPromptTemplate());
    }

    private List<AiModelBO> routeModels(AiScenarioBO scenario,
                                        AiUsageBindingBO binding,
                                        KnowledgeBaseGenerateRequest request) {
        List<AiModelBO> bindingModels = routeByUsageBinding(binding, request);
        if (!bindingModels.isEmpty()) {
            return bindingModels;
        }
        try {
            return List.of(modelRouteAppService.route(scenario, request == null ? null : request.getPreferredModelCode()));
        } catch (Exception ignored) {
            return List.of(mockModel());
        }
    }

    private List<AiModelBO> routeByUsageBinding(AiUsageBindingBO binding, KnowledgeBaseGenerateRequest request) {
        if (binding == null || binding.getProviderId() == null) {
            return List.of();
        }
        try {
            AiProviderBO provider = aiProviderRepository.findById(binding.getProviderId());
            if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
                return List.of();
            }
            List<String> modelNames = orderedModelNames(binding, provider, request);
            List<AiModelBO> models = new ArrayList<>();
            for (String modelName : modelNames) {
                models.add(buildProviderModel(provider, modelName));
            }
            return models.isEmpty() ? List.of() : models;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> orderedModelNames(AiUsageBindingBO binding,
                                           AiProviderBO provider,
                                           KnowledgeBaseGenerateRequest request) {
        Set<String> modelNames = new LinkedHashSet<>();
        String preferred = request == null ? "" : normalize(request.getPreferredModelCode());
        if (!preferred.isBlank()) {
            modelNames.add(preferred);
        }
        addIfPresent(modelNames, binding.getModelOverride());
        addStringArray(modelNames, binding.getParams(), "fallback_chain");
        addStringArray(modelNames, binding.getParams(), "fallback_models");
        addIfPresent(modelNames, provider.getDefaultModel());
        return new ArrayList<>(modelNames);
    }

    private AiModelBO buildProviderModel(AiProviderBO provider, String modelName) {
        String providerCode = normalize(provider.getProviderType()).isBlank()
                ? normalize(provider.getCode())
                : normalize(provider.getProviderType());
        return AiModelBO.builder()
                .modelCode(modelName)
                .providerCode(providerCode)
                .modelName(modelName)
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra())
                .inputPricePerMillion(provider.getInputPricePerMillion())
                .outputPricePerMillion(provider.getOutputPricePerMillion())
                .pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .enabled(true)
                .build();
    }

    private AiModelBO mockModel() {
        return AiModelBO.builder()
                .modelCode("mock-model")
                .providerCode("mock")
                .modelName("Mock Model")
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .enabled(true)
                .build();
    }

    private String buildPrompt(PromptRuntime promptRuntime, String sourceText) {
        String userPrompt = promptRuntime.userPromptTemplate()
                .replace("{{formatInstructions}}", outputParser.getFormat())
                .replace("{{sourceText}}", sourceText);
        return promptRuntime.systemPrompt() + System.lineSeparator() + userPrompt;
    }

    private String buildRepairPrompt(PromptRuntime promptRuntime,
                                     String sourceText,
                                     String rawOutput,
                                     Exception parseException) {
        return promptRuntime.systemPrompt()
                + System.lineSeparator()
                + "下面的模型输出没有通过结构化 JSON 校验。请只返回修复后的合法 JSON，不要解释。"
                + System.lineSeparator()
                + outputParser.getFormat()
                + System.lineSeparator()
                + "解析错误：" + parseException.getMessage()
                + System.lineSeparator()
                + "原始输入："
                + System.lineSeparator()
                + sourceText
                + System.lineSeparator()
                + "待修复输出："
                + System.lineSeparator()
                + rawOutput;
    }

    private void recordExecutionLog(String executionNo,
                                    PromptRuntime promptRuntime,
                                    AiModelBO model,
                                    String status,
                                    Instant startedAt,
                                    String sourceText,
                                    AiCompletionResultBO completion,
                                    Exception error) {
        try {
            aiExecutionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo(executionNo)
                    .capability(CAPABILITY)
                    .promptKey(promptRuntime.promptKey())
                    .promptVersion(promptRuntime.promptVersion())
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status(status)
                    .inputTokens(completion == null ? estimateTokens(sourceText) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? estimateTokens(sourceText) : completion.getTotalTokens())
                    .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                    .requestJson(toJson(Map.of(
                            "sourceTextPreview", truncate(sourceText, MAX_LOG_TEXT_LENGTH),
                            "sourceTextLength", sourceText == null ? 0 : sourceText.length()
                    )))
                    .responseJson(toJson(sanitizedResponse(completion)))
                    .errorMessage(error == null ? null : truncate(error.getMessage(), 2000))
                    .extra(toJson(Map.of("fallbackCapable", true)))
                    .build());
        } catch (Exception ignored) {
            // Logging must not break the user-facing generation flow.
        }
    }

    private Map<String, Object> sanitizedResponse(AiCompletionResultBO completion) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (completion == null) {
            return response;
        }
        response.put("providerCode", completion.getProviderCode());
        response.put("modelCode", completion.getModelCode());
        response.put("contentPreview", truncate(completion.getContent(), MAX_LOG_TEXT_LENGTH));
        response.put("rawResponsePreview", truncate(completion.getRawResponse(), MAX_LOG_TEXT_LENGTH));
        return response;
    }

    private KnowledgeBaseGenerateDTO normalizeResult(KnowledgeBaseGenerateDTO result, String sourceText) {
        KnowledgeBaseDocumentDraftPolicy.Draft draft = draftPolicy.normalize(
                result == null ? null : result.getTitle(), result == null ? null : result.getDescription(),
                result == null ? null : result.getTags(), result == null ? null : result.getCategories(),
                result == null ? null : result.getContent(), sourceText, null);
        return KnowledgeBaseGenerateDTO.builder()
                .title(draft.title()).description(draft.description()).tags(draft.tags())
                .categories(draft.categories()).content(draft.content()).build();
    }

    private KnowledgeBaseGenerateDTO fallbackResult(String sourceText, Exception error) {
        KnowledgeBaseDocumentDraftPolicy.Draft draft = draftPolicy.normalize(null, null, null, null, null,
                sourceText, error == null ? null : error.getMessage());
        return KnowledgeBaseGenerateDTO.builder()
                .title(draft.title()).description(draft.description()).tags(draft.tags())
                .categories(draft.categories()).content(draft.content())
                .build();
    }

    private void addIfPresent(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private void addStringArray(Set<String> values, String rawJson, String field) {
        JsonNode params = readJson(rawJson);
        JsonNode array = params.get(field);
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonNode item : array) {
            addIfPresent(values, item.asText(""));
        }
    }

    private String stringParam(String rawJson, String field) {
        JsonNode params = readJson(rawJson);
        JsonNode value = params.get(field);
        return value == null || value.isNull() ? "" : normalize(value.asText(""));
    }

    private JsonNode readJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record PromptRuntime(String promptKey,
                                 String promptVersion,
                                 String systemPrompt,
                                 String userPromptTemplate) {
    }
}
