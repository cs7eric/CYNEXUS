package cn.cccs7.cynexus.ai.domain.knowledgegeneration.impl;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationDomainService;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseDocumentGenerationPromptPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationAttempt;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationCommand;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationOutcome;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationParsedDocument;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationPrompt;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationRoutingParameters;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.KnowledgeBaseGenerationStatus;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationCompletionPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationFallbackModelPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationOutputParserPort;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationRoutingParametersPort;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class KnowledgeBaseDocumentGenerationDomainServiceImpl
        implements KnowledgeBaseDocumentGenerationDomainService {
    private static final int MAX_REPAIR_OUTPUT_LENGTH = 8000;
    private static final int MAX_LOG_TEXT_LENGTH = 8000;

    private final AiUsageBindingRepository bindingRepository;
    private final AiProviderRepository providerRepository;
    private final AiPromptRepository promptRepository;
    private final AiExecutionLogRepository executionLogRepository;
    private final KnowledgeBaseGenerationFallbackModelPort fallbackModelPort;
    private final KnowledgeBaseGenerationRoutingParametersPort routingParametersPort;
    private final KnowledgeBaseGenerationCompletionPort completionPort;
    private final KnowledgeBaseGenerationOutputParserPort outputParserPort;
    private final KnowledgeBaseDocumentDraftPolicy draftPolicy;
    private final KnowledgeBaseDocumentGenerationPromptPolicy promptPolicy;

    public KnowledgeBaseDocumentGenerationDomainServiceImpl(AiUsageBindingRepository bindingRepository,
                                                              AiProviderRepository providerRepository,
                                                              AiPromptRepository promptRepository,
                                                              AiExecutionLogRepository executionLogRepository,
                                                              KnowledgeBaseGenerationFallbackModelPort fallbackModelPort,
                                                              KnowledgeBaseGenerationRoutingParametersPort routingParametersPort,
                                                              KnowledgeBaseGenerationCompletionPort completionPort,
                                                              KnowledgeBaseGenerationOutputParserPort outputParserPort,
                                                              KnowledgeBaseDocumentDraftPolicy draftPolicy,
                                                              KnowledgeBaseDocumentGenerationPromptPolicy promptPolicy) {
        this.bindingRepository = bindingRepository;
        this.providerRepository = providerRepository;
        this.promptRepository = promptRepository;
        this.executionLogRepository = executionLogRepository;
        this.fallbackModelPort = fallbackModelPort;
        this.routingParametersPort = routingParametersPort;
        this.completionPort = completionPort;
        this.outputParserPort = outputParserPort;
        this.draftPolicy = draftPolicy;
        this.promptPolicy = promptPolicy;
    }

    @Override
    public KnowledgeBaseGenerationOutcome generate(KnowledgeBaseGenerationCommand command) {
        String sourceText = normalize(command == null ? null : command.sourceText());
        if (sourceText.isBlank()) {
            return new KnowledgeBaseGenerationOutcome(KnowledgeBaseGenerationStatus.INVALID_INPUT, sourceText,
                    null, null, List.of(), "sourceText is required");
        }
        sourceText = draftPolicy.truncateSource(sourceText);
        AiUsageBindingBO binding;
        KnowledgeBaseGenerationPrompt prompt;
        List<AiModelBO> models;
        try {
            binding = resolveBinding(command);
            prompt = resolvePrompt(binding);
            models = resolveModels(binding, command);
        } catch (Exception resolutionFailure) {
            return failed(sourceText, null, List.of(), message(resolutionFailure));
        }
        if (models.isEmpty()) {
            return failed(sourceText, prompt, List.of(), "No available model for knowledge document generation");
        }

        String requestPrompt = buildPrompt(prompt, sourceText);
        List<KnowledgeBaseGenerationAttempt> attempts = new ArrayList<>();
        String lastFailure = "All configured AI routes failed";
        for (AiModelBO model : models) {
            Instant startedAt = Instant.now();
            String executionNo = "AI-" + UUID.randomUUID().toString().replace("-", "");
            AiCompletionResultBO completion = null;
            try {
                completion = completionPort.complete(model, requestPrompt,
                        KnowledgeBaseDocumentGenerationPromptPolicy.CAPABILITY);
            } catch (Exception completionFailure) {
                lastFailure = message(completionFailure);
                attempts.add(KnowledgeBaseGenerationAttempt.failed(executionNo, model, prompt, startedAt,
                        null, completionFailure));
                continue;
            }
            try {
                KnowledgeBaseGenerationParsedDocument parsed = parse(completion);
                attempts.add(KnowledgeBaseGenerationAttempt.succeeded(executionNo, model, prompt, startedAt, completion));
                return succeeded(sourceText, prompt, attempts, parsed);
            } catch (Exception parseFailure) {
                lastFailure = message(parseFailure);
                try {
                    AiCompletionResultBO repaired = completionPort.complete(model,
                            buildRepairPrompt(prompt, sourceText, completion.getContent(), parseFailure),
                            KnowledgeBaseDocumentGenerationPromptPolicy.CAPABILITY);
                    KnowledgeBaseGenerationParsedDocument parsed = parse(repaired);
                    attempts.add(KnowledgeBaseGenerationAttempt.succeeded(executionNo, model, prompt, startedAt, repaired));
                    return succeeded(sourceText, prompt, attempts, parsed);
                } catch (Exception repairFailure) {
                    lastFailure = message(repairFailure);
                    attempts.add(KnowledgeBaseGenerationAttempt.failed(executionNo, model, prompt, startedAt,
                            completion, repairFailure));
                }
            }
        }
        return failed(sourceText, prompt, attempts, lastFailure);
    }

    @Override
    public void recordExecutionLogs(KnowledgeBaseGenerationOutcome outcome) {
        if (outcome == null || outcome.attempts() == null) return;
        ObjectMapper objectMapper = new ObjectMapper();
        for (KnowledgeBaseGenerationAttempt attempt : outcome.attempts()) {
            try {
                AiModelBO model = attempt.model();
                AiCompletionResultBO completion = attempt.completion();
                String sourceText = outcome.sourceText();
                executionLogRepository.create(AiExecutionLogBO.builder()
                        .executionNo(attempt.executionNo()).capability("knowledge_base_document_generate")
                        .promptKey(attempt.prompt() == null ? null : attempt.prompt().promptKey())
                        .promptVersion(attempt.prompt() == null ? null : attempt.prompt().promptVersion())
                        .providerCode(model == null ? null : model.getProviderCode())
                        .modelCode(model == null ? null : model.getModelCode()).status(attempt.status())
                        .inputTokens(completion == null ? estimateTokens(sourceText) : completion.getInputTokens())
                        .outputTokens(completion == null ? 0 : completion.getOutputTokens())
                        .totalTokens(completion == null ? estimateTokens(sourceText) : completion.getTotalTokens())
                        .latencyMs(attempt.latencyMs())
                        .requestJson(toJson(objectMapper, Map.of("sourceTextPreview", truncate(sourceText, MAX_LOG_TEXT_LENGTH),
                                "sourceTextLength", sourceText == null ? 0 : sourceText.length())))
                        .responseJson(toJson(objectMapper, Map.of("providerCode", safe(model == null ? null : model.getProviderCode()),
                                "modelCode", safe(model == null ? null : model.getModelCode()),
                                "contentPreview", safe(truncate(completion == null ? null : completion.getContent(), MAX_LOG_TEXT_LENGTH)),
                                "rawResponsePreview", safe(truncate(completion == null ? null : completion.getRawResponse(), MAX_LOG_TEXT_LENGTH)))))
                        .errorMessage(attempt.errorMessage() == null ? null : truncate(attempt.errorMessage(), 2000))
                        .extra(toJson(objectMapper, Map.of("fallbackCapable", true))).build());
            } catch (Exception ignored) {
                // Execution logging must not change the generation outcome.
            }
        }
    }

    private String toJson(ObjectMapper mapper, Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ignored) { return "{}"; }
    }

    private int estimateTokens(String value) { return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4); }

    private String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String safe(String value) { return value == null ? "" : value; }

    private AiUsageBindingBO resolveBinding(KnowledgeBaseGenerationCommand command) {
        List<AiUsageBindingBO> bindings = bindingRepository.listAll();
        if (bindings == null) {
            return null;
        }
        return bindings.stream().filter(binding -> binding != null)
                .filter(binding -> KnowledgeBaseDocumentGenerationPromptPolicy.CAPABILITY.equals(binding.getCapability()))
                .filter(binding -> Boolean.TRUE.equals(binding.getEnabled()))
                .filter(binding -> routingParameters(binding).matches(command))
                .findFirst().orElse(null);
    }

    private KnowledgeBaseGenerationRoutingParameters routingParameters(AiUsageBindingBO binding) {
        if (binding == null) {
            return KnowledgeBaseGenerationRoutingParameters.empty();
        }
        try {
            KnowledgeBaseGenerationRoutingParameters value = routingParametersPort.parse(binding.getParams());
            return value == null ? KnowledgeBaseGenerationRoutingParameters.empty() : value;
        } catch (Exception ignored) {
            return KnowledgeBaseGenerationRoutingParameters.empty();
        }
    }

    private KnowledgeBaseGenerationPrompt resolvePrompt(AiUsageBindingBO binding) {
        AiPromptBO bound = binding == null || binding.getPromptId() == null
                ? null : promptRepository.findById(binding.getPromptId());
        String version = routingParameters(binding).promptVersion();
        List<AiPromptBO> prompts = promptRepository.list(
                KnowledgeBaseDocumentGenerationPromptPolicy.PROMPT_KEY, null, "active");
        return promptPolicy.resolve(bound, prompts, version);
    }

    private List<AiModelBO> resolveModels(AiUsageBindingBO binding, KnowledgeBaseGenerationCommand command) {
        List<AiModelBO> configured = resolveBindingModels(binding, command);
        if (!configured.isEmpty()) {
            return configured;
        }
        try {
            AiModelBO fallback = fallbackModelPort.route(command == null ? null : command.preferredModelCode());
            return fallback == null ? List.of() : List.of(fallback);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<AiModelBO> resolveBindingModels(AiUsageBindingBO binding, KnowledgeBaseGenerationCommand command) {
        if (binding == null || binding.getProviderId() == null) {
            return List.of();
        }
        AiProviderBO provider = providerRepository.findById(binding.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            return List.of();
        }
        KnowledgeBaseGenerationRoutingParameters parameters = routingParameters(binding);
        Set<String> names = new LinkedHashSet<>();
        add(names, command == null ? null : command.preferredModelCode());
        add(names, binding.getModelOverride());
        if (parameters.fallbackModels() != null) {
            parameters.fallbackModels().forEach(value -> add(names, value));
        }
        add(names, provider.getDefaultModel());
        List<AiModelBO> models = new ArrayList<>();
        names.forEach(name -> models.add(toModel(provider, name)));
        return models;
    }

    private AiModelBO toModel(AiProviderBO provider, String name) {
        String providerCode = normalize(provider.getProviderType());
        if (providerCode.isBlank()) {
            providerCode = normalize(provider.getCode());
        }
        return AiModelBO.builder().modelCode(name).providerCode(providerCode).modelName(name)
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra()).inputPricePerMillion(provider.getInputPricePerMillion())
                .outputPricePerMillion(provider.getOutputPricePerMillion()).pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder().structuredOutputSupported(true).build()).enabled(true).build();
    }

    private KnowledgeBaseGenerationParsedDocument parse(AiCompletionResultBO completion) throws Exception {
        if (completion == null) {
            throw new IllegalStateException("AI completion was empty");
        }
        return outputParserPort.parse(completion.getContent());
    }

    private String buildPrompt(KnowledgeBaseGenerationPrompt prompt, String sourceText) {
        return prompt.systemPrompt() + System.lineSeparator() + prompt.userPromptTemplate()
                .replace("{{formatInstructions}}", outputParserPort.formatInstructions())
                .replace("{{sourceText}}", sourceText);
    }

    private String buildRepairPrompt(KnowledgeBaseGenerationPrompt prompt, String sourceText,
                                     String rawOutput, Exception failure) {
        return prompt.systemPrompt() + System.lineSeparator()
                + "下面的模型输出没有通过结构化 JSON 校验。请只返回修复后的合法 JSON，不要解释。"
                + System.lineSeparator() + outputParserPort.formatInstructions()
                + System.lineSeparator() + "解析错误：" + truncate(message(failure), 500)
                + System.lineSeparator() + "原始输入：" + System.lineSeparator() + sourceText
                + System.lineSeparator() + "待修复输出：" + System.lineSeparator() + truncate(rawOutput, MAX_REPAIR_OUTPUT_LENGTH);
    }

    private KnowledgeBaseGenerationOutcome succeeded(String sourceText, KnowledgeBaseGenerationPrompt prompt,
                                                      List<KnowledgeBaseGenerationAttempt> attempts,
                                                      KnowledgeBaseGenerationParsedDocument parsed) {
        KnowledgeBaseDocumentDraftPolicy.Draft draft = draftPolicy.normalize(parsed.title(), parsed.description(),
                parsed.tags(), parsed.categories(), parsed.content(), sourceText, null);
        return new KnowledgeBaseGenerationOutcome(KnowledgeBaseGenerationStatus.SUCCEEDED, sourceText, prompt,
                draft, List.copyOf(attempts), null);
    }

    private KnowledgeBaseGenerationOutcome failed(String sourceText, KnowledgeBaseGenerationPrompt prompt,
                                                  List<KnowledgeBaseGenerationAttempt> attempts, String reason) {
        return new KnowledgeBaseGenerationOutcome(KnowledgeBaseGenerationStatus.FAILED, sourceText, prompt,
                null, List.copyOf(attempts), reason);
    }

    private void add(Set<String> names, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            names.add(normalized);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String message(Exception failure) {
        String value = failure == null ? "generation failed" : normalize(failure.getMessage());
        return value.isBlank() ? (failure == null ? "generation failed" : failure.getClass().getSimpleName()) : value;
    }

}
