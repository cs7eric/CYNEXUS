package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.KnowledgeBaseGenerateDTO;
import cn.cccs7.cynexus.ai.api.request.KnowledgeBaseGenerateRequest;
import cn.cccs7.cynexus.ai.application.prompt.KnowledgeBaseStructuredOutputParser;
import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AgentExecutor;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationDomainService;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationAttempt;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationCommand;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationOutcome;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationRoutingParameters;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationStatus;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.impl.KnowledgeBaseDocumentGenerationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationCompletionPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationFallbackModelPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationOutputParserPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationRoutingParametersPort;
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Application adapter for the knowledge-document generation use case. */
@Service
public class KnowledgeBaseDocumentGenerationAppService {

    private static final int MAX_LOG_TEXT_LENGTH = 8000;

    private final ModelRouteAppService modelRouteAppService;
    private final AgentExecutor agentExecutor;
    private final AiExecutionLogRepository aiExecutionLogRepository;
    private final KnowledgeBaseDocumentGenerationDomainService generationDomainService;
    private final KnowledgeBaseStructuredOutputParser outputParser;
    private final StructuredJsonExtractor structuredJsonExtractor;
    private final ObjectMapper objectMapper;

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
        this.aiExecutionLogRepository = aiExecutionLogRepository;
        this.structuredJsonExtractor = structuredJsonExtractor;
        this.outputParser = outputParser;
        this.objectMapper = new ObjectMapper();
        this.generationDomainService = new KnowledgeBaseDocumentGenerationDomainServiceImpl(
                aiUsageBindingRepository,
                aiProviderRepository,
                aiPromptRepository,
                fallbackModelPort(),
                routingParametersPort(),
                completionPort(),
                outputParserPort(),
                new cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy(),
                new cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationPromptPolicy());
    }

    public KnowledgeBaseGenerateDTO generate(KnowledgeBaseGenerateRequest request) {
        KnowledgeBaseGenerationOutcome outcome = generationDomainService.generate(new KnowledgeBaseGenerationCommand(
                request == null ? null : request.getSourceText(),
                request == null ? null : request.getPreferredModelCode(),
                request == null ? null : request.getTenantCode(),
                request == null ? null : request.getRoutingQuality(),
                request == null ? null : request.getMaxCostTier()));

        recordExecutionLogs(outcome);
        if (outcome.status() == KnowledgeBaseGenerationStatus.INVALID_INPUT) {
            throw new BizException(ErrorCode.BAD_REQUEST, outcome.failureReason());
        }
        if (!outcome.succeeded()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "Knowledge Base document generation failed: " + safeMessage(outcome.failureReason()));
        }

        KnowledgeBaseDocumentDraftPolicy.Draft draft = outcome.draft();
        return KnowledgeBaseGenerateDTO.builder()
                .title(draft.title())
                .description(draft.description())
                .tags(draft.tags())
                .categories(draft.categories())
                .content(draft.content())
                .build();
    }

    private KnowledgeBaseGenerationFallbackModelPort fallbackModelPort() {
        return preferredModelCode -> modelRouteAppService.route(buildScenario(), preferredModelCode);
    }

    private KnowledgeBaseGenerationCompletionPort completionPort() {
        return (model, prompt, capability) -> agentExecutor.completeDetailed(model, prompt, capability);
    }

    private KnowledgeBaseGenerationOutputParserPort outputParserPort() {
        return new KnowledgeBaseGenerationOutputParserPort() {
            @Override
            public String formatInstructions() {
                return outputParser.getFormat();
            }

            @Override
            public KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception {
                KnowledgeBaseGenerateDTO parsed = outputParser.convert(structuredJsonExtractor.extractObject(rawOutput));
                return new KnowledgeBaseGenerationParsedDocument(
                        parsed.getTitle(), parsed.getDescription(), parsed.getTags(),
                        parsed.getCategories(), parsed.getContent());
            }
        };
    }

    private KnowledgeBaseGenerationRoutingParametersPort routingParametersPort() {
        return rawParams -> {
            JsonNode params = readJson(rawParams);
            return new KnowledgeBaseGenerationRoutingParameters(
                    normalizedValues(params.get("tenant_code")),
                    normalizedValues(params.get("quality")),
                    normalizedValues(params.get("cost_tier")),
                    normalizedScalar(params.get("prompt_version")),
                    orderedValues(params, "fallback_chain", "fallback_models"));
        };
    }

    private AiScenarioBO buildScenario() {
        return AiScenarioBO.builder()
                .scenarioCode("knowledge_base_document_generate")
                .scenarioName("Knowledge Base Document Generation")
                .maxToolCalls(0)
                .toolCallRequired(false)
                .jsonOutputRequired(true)
                .enabled(true)
                .build();
    }

    private void recordExecutionLogs(KnowledgeBaseGenerationOutcome outcome) {
        if (outcome == null || outcome.attempts() == null) {
            return;
        }
        for (KnowledgeBaseGenerationAttempt attempt : outcome.attempts()) {
            recordExecutionLog(outcome.sourceText(), attempt);
        }
    }

    private void recordExecutionLog(String sourceText, KnowledgeBaseGenerationAttempt attempt) {
        try {
            AiModelBO model = attempt.model();
            AiCompletionResultBO completion = attempt.completion();
            String promptKey = attempt.prompt() == null ? null : attempt.prompt().promptKey();
            String promptVersion = attempt.prompt() == null ? null : attempt.prompt().promptVersion();
            aiExecutionLogRepository.create(AiExecutionLogBO.builder()
                    .executionNo(attempt.executionNo())
                    .capability("knowledge_base_document_generate")
                    .promptKey(promptKey)
                    .promptVersion(promptVersion)
                    .providerCode(model == null ? null : model.getProviderCode())
                    .modelCode(model == null ? null : model.getModelCode())
                    .status(attempt.status())
                    .inputTokens(completion == null ? estimateTokens(sourceText) : completion.getInputTokens())
                    .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                    .totalTokens(completion == null ? estimateTokens(sourceText) : completion.getTotalTokens())
                    .latencyMs(attempt.latencyMs())
                    .requestJson(toJson(Map.of(
                            "sourceTextPreview", truncate(sourceText, MAX_LOG_TEXT_LENGTH),
                            "sourceTextLength", sourceText == null ? 0 : sourceText.length())))
                    .responseJson(toJson(sanitizedResponse(completion)))
                    .errorMessage(attempt.errorMessage() == null ? null : truncate(attempt.errorMessage(), 2000))
                    .extra(toJson(Map.of("fallbackCapable", true)))
                    .build());
        } catch (Exception ignored) {
            // Logging must not change the outcome of the generation use case.
        }
    }

    private Set<String> normalizedValues(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node == null || node.isNull()) {
            return values;
        }
        if (node.isArray()) {
            node.forEach(item -> addNormalized(values, item.asText("")));
        } else {
            addNormalized(values, node.asText(""));
        }
        return values;
    }

    private List<String> orderedValues(JsonNode params, String... fields) {
        Set<String> values = new LinkedHashSet<>();
        for (String field : fields) {
            JsonNode node = params == null ? null : params.get(field);
            if (node == null || !node.isArray()) {
                continue;
            }
            node.forEach(item -> addNormalized(values, item.asText("")));
        }
        return new ArrayList<>(values);
    }

    private void addNormalized(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized.toLowerCase());
        }
    }

    private String normalizedScalar(JsonNode node) {
        return node == null || node.isNull() ? "" : normalize(node.asText(""));
    }

    private JsonNode readJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode value = objectMapper.readTree(rawJson);
            return value == null || !value.isObject() ? objectMapper.createObjectNode() : value;
        } catch (JsonProcessingException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, Object> sanitizedResponse(AiCompletionResultBO completion) {
        if (completion == null) {
            return Map.of();
        }
        return Map.of(
                "providerCode", safeValue(completion.getProviderCode()),
                "modelCode", safeValue(completion.getModelCode()),
                "contentPreview", safeValue(truncate(completion.getContent(), MAX_LOG_TEXT_LENGTH)),
                "rawResponsePreview", safeValue(truncate(completion.getRawResponse(), MAX_LOG_TEXT_LENGTH)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
    }

    private String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String safeMessage(String value) {
        return value == null || value.isBlank() ? "no available AI route" : truncate(value, 500);
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
