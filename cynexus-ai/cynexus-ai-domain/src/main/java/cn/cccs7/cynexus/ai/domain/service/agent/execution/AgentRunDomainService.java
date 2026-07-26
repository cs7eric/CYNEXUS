package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentClarificationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.factory.citation.AgentCitationFactory;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentCapabilityDescriptorPolicy;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentHistoryCollapsePolicy;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentReasoningPolicy;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentFinalAnswerPolicy;
import cn.cccs7.cynexus.ai.domain.policy.provider.AiProviderModelPricingPolicy;
import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSensitiveDataPolicy;
import cn.cccs7.cynexus.ai.domain.policy.sensitivedata.AgentSourceUrlPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import cn.cccs7.cynexus.ai.domain.valueobject.citation.AgentCitation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Agent run aggregate orchestration. Application code supplies API mapping and
 * provider/tool ports; this service owns persistence, routing, and run-state transitions.
 */
@Service
public class AgentRunDomainService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AgentRunRepository runRepository;
    private final AgentCapabilityRepository capabilityRepository;
    private final AiProviderRepository providerRepository;
    private final AiUsageBindingRepository usageBindingRepository;

    public AgentRunDomainService(AgentRunRepository runRepository,
                                 AgentCapabilityRepository capabilityRepository,
                                 AiProviderRepository providerRepository,
                                 AiUsageBindingRepository usageBindingRepository) {
        this.runRepository = runRepository;
        this.capabilityRepository = capabilityRepository;
        this.providerRepository = providerRepository;
        this.usageBindingRepository = usageBindingRepository;
    }

    public Optional<AgentRunBO> findRunByIdempotencyKey(String ownerKey, String idempotencyKey) {
        return runRepository.findRunByIdempotencyKey(ownerKey, idempotencyKey);
    }

    public Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo) {
        return runRepository.findConversation(ownerKey, conversationNo);
    }

    public void saveConversation(AgentConversationBO conversation) {
        runRepository.saveConversation(conversation);
    }

    public void saveRun(AgentRunBO run) {
        runRepository.saveRun(run);
    }

    public void updateRun(AgentRunBO run) {
        runRepository.updateRun(run);
    }

    public void updateConversation(AgentConversationBO conversation) {
        runRepository.updateConversation(conversation);
    }

    public boolean archiveConversation(String ownerKey, String conversationNo) {
        if (ownerKey == null || ownerKey.isBlank() || conversationNo == null || conversationNo.isBlank()) {
            throw new IllegalArgumentException("Agent conversation identity is required");
        }
        return runRepository.archiveConversation(ownerKey, conversationNo, OffsetDateTime.now());
    }

    public void saveStep(AgentRunStepBO step) {
        runRepository.saveStep(step);
    }

    public AgentRunStepBO saveToolStep(String ownerKey, String runNo, int stepNo, AgentToolCall call,
                                       AgentToolResult result, int attemptCount, String sourceUrl,
                                       String freshness, String failureCategory) {
        String resolvedSourceUrl = result.getSourceUrl() == null ? sourceUrl : result.getSourceUrl();
        resolvedSourceUrl = AgentSourceUrlPolicy.sanitize(resolvedSourceUrl, Set.of());
        List<AgentCitation> citations = result.getCitations() == null || result.getCitations().isEmpty()
                ? AgentCitationFactory.fromTool(call.getToolCode(), result.getSource(), resolvedSourceUrl,
                result.getDataDate(), result.getSummary(), freshness)
                : result.getCitations().stream().map(this::sanitizeCitation).toList();
        AgentRunStepBO step = AgentRunStepBO.builder()
                .ownerKey(ownerKey)
                .runNo(runNo)
                .stepNo(stepNo)
                .kind("TOOL")
                .attemptCount(attemptCount)
                .toolCode(call.getToolCode())
                .inputJson(AgentSensitiveDataPolicy.redactJson(json(call.getArguments())))
                .outputJson(AgentSensitiveDataPolicy.redactJson(result.getOutputJson()))
                .status(result.getStatus())
                .summary(AgentSensitiveDataPolicy.redactText(result.getSummary()))
                .source(result.getSource())
                .sourceUrl(resolvedSourceUrl)
                .citationJson(AgentSensitiveDataPolicy.redactJson(json(citations)))
                .dataDate(result.getDataDate())
                .errorMessage(result.getWarnings() == null || result.getWarnings().isEmpty()
                        ? null : AgentSensitiveDataPolicy.redactText(String.join(",", result.getWarnings())))
                .failureCategory("SUCCEEDED".equals(result.getStatus()) ? null : failureCategory)
                .startedAt(OffsetDateTime.now())
                .finishedAt(OffsetDateTime.now())
                .build();
        runRepository.saveStep(step);
        return step;
    }

    public AgentRunStepBO saveThinkingStep(String ownerKey, String runNo, int stepNo, String thinking) {
        String text = AgentSensitiveDataPolicy.redactText(valueOr(thinking, "").trim());
        if (text.length() > 12_000) text = text.substring(0, 12_000) + "...";
        String summary = text.length() <= 180 ? text : text.substring(0, 180) + "...";
        AgentRunStepBO step = AgentRunStepBO.builder()
                .ownerKey(ownerKey)
                .runNo(runNo)
                .stepNo(stepNo)
                .kind("THINKING")
                .status("SUCCEEDED")
                .summary(summary)
                .outputJson(AgentSensitiveDataPolicy.redactJson(json(Map.of(
                        "type", "thinking",
                        "content", text
                ))))
                .startedAt(OffsetDateTime.now())
                .finishedAt(OffsetDateTime.now())
                .build();
        runRepository.saveStep(step);
        return step;
    }

    public AgentRunStepBO saveAnswerStep(String ownerKey, String runNo, int stepNo, String answer) {
        String safeAnswer = AgentSensitiveDataPolicy.redactText(answer);
        AgentRunStepBO step = AgentRunStepBO.builder()
                .ownerKey(ownerKey)
                .runNo(runNo)
                .stepNo(stepNo)
                .kind("ANSWER")
                .status("SUCCEEDED")
                .summary("Generated the final Agent answer.")
                .outputJson(json(Map.of("content", safeAnswer)))
                .startedAt(OffsetDateTime.now())
                .finishedAt(OffsetDateTime.now())
                .build();
        runRepository.saveStep(step);
        return step;
    }

    public AgentRunStepBO saveClarificationStep(String ownerKey, String runNo, int stepNo,
                                                AgentClarificationBO clarification) {
        AgentRunStepBO step = AgentRunStepBO.builder()
                .ownerKey(ownerKey)
                .runNo(runNo)
                .stepNo(stepNo)
                .kind("CLARIFICATION")
                .status("NEEDS_CLARIFICATION")
                .summary(AgentSensitiveDataPolicy.redactText(clarification.getSummary()))
                .outputJson(AgentSensitiveDataPolicy.redactJson(json(clarification)))
                .startedAt(OffsetDateTime.now())
                .finishedAt(OffsetDateTime.now())
                .build();
        runRepository.saveStep(step);
        return step;
    }

    public void saveMessage(AgentMessageBO message) {
        runRepository.saveMessage(message);
    }

    public void saveMessage(String conversationNo, String runNo, int sequenceNo, AgentModelMessage message) {
        runRepository.saveMessage(AgentMessageBO.builder()
                .conversationNo(conversationNo)
                .runNo(runNo)
                .sequenceNo(sequenceNo)
                .role(message.getRole())
                .content(AgentSensitiveDataPolicy.redactText(message.getContent()))
                .toolCallId(message.getToolCallId())
                .toolName(message.getToolName())
                .toolCallsJson(AgentSensitiveDataPolicy.redactJson(json(
                        message.getToolCalls() == null ? List.of() : message.getToolCalls())))
                .runtimeContext(AgentSensitiveDataPolicy.redactText(message.getRuntimeContext()))
                .providerOutputJson(AgentSensitiveDataPolicy.redactJson(message.getProviderOutputJson()))
                .build());
    }

    public AgentModelMessage toModelMessage(AgentMessageBO message) {
        List<AgentModelToolCall> calls = List.of();
        if (message.getToolCallsJson() != null && !message.getToolCallsJson().isBlank()) {
            try {
                calls = OBJECT_MAPPER.readValue(message.getToolCallsJson(), OBJECT_MAPPER.getTypeFactory()
                        .constructCollectionType(List.class, AgentModelToolCall.class));
            } catch (Exception ignored) {
                calls = List.of();
            }
        }
        return AgentModelMessage.builder()
                .role(message.getRole())
                .content(valueOr(message.getContent(), ""))
                .toolCallId(message.getToolCallId())
                .toolName(message.getToolName())
                .toolCalls(calls)
                .runtimeContext(message.getRuntimeContext())
                .providerOutputJson(message.getProviderOutputJson())
                .build();
    }

    public List<AgentModelMessage> collapsePersistedHistoryForProvider(
            String ownerKey, List<AgentMessageBO> persistedMessages) {
        if (persistedMessages == null || persistedMessages.isEmpty()) return List.of();
        Set<String> inspectedRunNos = new LinkedHashSet<>();
        Set<String> replayableRunNos = new LinkedHashSet<>();
        for (AgentMessageBO message : persistedMessages) {
            if (message == null || message.getRunNo() == null || message.getRunNo().isBlank()
                    || !inspectedRunNos.add(message.getRunNo())) {
                continue;
            }
            runRepository.findRun(ownerKey, message.getRunNo())
                    .filter(run -> AiConstants.AGENT_RUN_STATUS_SUCCEEDED.equals(run.getStatus())
                            || AiConstants.AGENT_RUN_STATUS_NEEDS_CLARIFICATION.equals(run.getStatus()))
                    .ifPresent(run -> replayableRunNos.add(message.getRunNo()));
        }
        return AgentHistoryCollapsePolicy.collapsePersistedForProvider(persistedMessages, replayableRunNos);
    }

    public static int nextMessageSequence(List<AgentMessageBO> persistedMessages) {
        if (persistedMessages == null || persistedMessages.isEmpty()) return 0;
        int maxSequence = 0;
        for (AgentMessageBO message : persistedMessages) {
            if (message == null || message.getSequenceNo() == null) continue;
            maxSequence = Math.max(maxSequence, message.getSequenceNo());
        }
        return maxSequence;
    }

    public List<AgentMessageBO> findMessages(String ownerKey, String conversationNo) {
        return runRepository.findMessages(ownerKey, conversationNo).stream().map(message -> {
            if (message != null && "assistant".equalsIgnoreCase(message.getRole())
                    && message.getContent() != null && !message.getContent().isBlank()) {
                message.setContent(AgentFinalAnswerPolicy.normalize(message.getContent()));
            }
            return message;
        }).toList();
    }

    public List<AgentRunStepBO> findSteps(String ownerKey, String runNo) {
        return runRepository.findSteps(ownerKey, runNo);
    }

    public void saveEvent(String ownerKey, AgentRunEventBO event) {
        runRepository.saveEvent(ownerKey, event);
    }

    public Optional<AgentRunBO> findRun(String ownerKey, String runNo) {
        return runRepository.findRun(ownerKey, runNo).map(run -> {
            if (run.getAnswer() != null && !run.getAnswer().isBlank()) {
                run.setAnswer(AgentFinalAnswerPolicy.normalize(run.getAnswer()));
            }
            return run;
        });
    }

    public Optional<AgentRunBO> requestCancellation(String ownerKey, String runNo, String requestedBy) {
        return runRepository.requestCancellation(ownerKey, runNo, requestedBy);
    }

    public boolean isCancellationRequested(String ownerKey, String runNo) {
        return runRepository.isCancellationRequested(ownerKey, runNo);
    }

    public List<AgentRunEventBO> findEvents(String ownerKey, String runNo, int afterSequenceNo, int limit) {
        return runRepository.findEvents(ownerKey, runNo, afterSequenceNo, limit);
    }

    public List<AgentConversationBO> listConversations(String ownerKey, int limit, String query) {
        return runRepository.listConversations(ownerKey, limit, query);
    }

    public List<AgentCapabilityDescriptor> listCapabilities() {
        return capabilityRepository.listAll();
    }

    public boolean updateCapability(String code, boolean enabled, String availability, int priority) {
        return capabilityRepository.updateGovernance(code, enabled, availability, priority);
    }

    public AgentCapabilityDescriptor register(AgentCapabilityDescriptor descriptor) {
        AgentCapabilityDescriptorPolicy.validateRegistration(descriptor);
        return capabilityRepository.register(descriptor);
    }

    public AgentCapabilityDescriptor registerCapability(AgentCapabilityDescriptor descriptor) {
        AgentCapabilityDescriptorPolicy.validateRegistration(descriptor);
        return capabilityRepository.register(descriptor);
    }

    public AiModelBO resolveModel(String preferredModelCode) {
        return resolveModel(null, preferredModelCode);
    }

    public AiModelBO resolveModel(String preferredProviderCode, String preferredModelCode) {
        return resolveModel(preferredProviderCode, preferredModelCode, null);
    }

    public AiModelBO resolveModel(String preferredProviderCode, String preferredModelCode,
                                  String preferredReasoningEffort) {
        List<AiUsageBindingBO> bindings = usageBindingRepository.listAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled())).toList();
        AiUsageBindingBO binding = firstBinding(bindings, AiConstants.AUTONOMOUS_AGENT_CAPABILITY)
                .or(() -> firstBinding(bindings, AiConstants.WORKSPACE_CHAT_CAPABILITY))
                .or(() -> firstBinding(bindings, AiConstants.TEXT_SUMMARY_CAPABILITY))
                .orElseThrow(() -> new IllegalStateException("No AI provider is bound to " + AiConstants.AUTONOMOUS_AGENT_CAPABILITY));
        AiProviderBO boundProvider = providerRepository.findById(binding.getProviderId());
        AiProviderBO provider = hasText(preferredProviderCode)
                ? providerRepository.listAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> preferredProviderCode.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The requested AI provider is unavailable"))
                : boundProvider;
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("The AI provider bound to autonomous_agent is unavailable");
        }
        String modelCode = hasText(preferredModelCode)
                ? preferredModelCode.trim()
                : hasText(preferredProviderCode)
                ? provider.getDefaultModel()
                : hasText(binding.getModelOverride()) ? binding.getModelOverride() : provider.getDefaultModel();
        List<AiProviderModelPricingPolicy.ModelPricing> configuredModels = AiProviderModelPricingPolicy.configuredModels(provider);
        AiProviderModelPricingPolicy.ModelPricing selectedModel = AiProviderModelPricingPolicy
                .find(provider, modelCode).orElse(null);
        if (!configuredModels.isEmpty() && selectedModel == null) {
            throw new IllegalArgumentException("The requested AI model is not configured for provider: " + provider.getCode());
        }
        if (!hasText(modelCode) && !configuredModels.isEmpty()) {
            selectedModel = configuredModels.get(0);
            modelCode = selectedModel.model();
        }
        BigDecimal inputPrice = selectedModel == null
                ? provider.getInputPricePerMillion() : selectedModel.inputPricePerMillion();
        BigDecimal outputPrice = selectedModel == null
                ? provider.getOutputPricePerMillion() : selectedModel.outputPricePerMillion();
        return AiModelBO.builder().modelCode(modelCode).modelName(modelCode)
                .providerIdentity(provider.getCode())
                .providerCode(provider.getProviderType() == null || provider.getProviderType().isBlank()
                        ? provider.getCode() : provider.getProviderType())
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(AgentReasoningPolicy.resolveRuntimeExtra(provider.getExtra(), binding.getParams(), preferredReasoningEffort)).enabled(true)
                .inputPricePerMillion(inputPrice).outputPricePerMillion(outputPrice)
                .pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder().structuredOutputSupported(true).toolCallSupported(true).build()).build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Optional<AiUsageBindingBO> firstBinding(List<AiUsageBindingBO> bindings, String capability) {
        return bindings.stream().filter(item -> capability.equals(item.getCapability())).findFirst();
    }

    private AgentCitation sanitizeCitation(AgentCitation citation) {
        if (citation == null) return null;
        String safeUrl = AgentSourceUrlPolicy.sanitize(citation.getUrl(), Set.of());
        return AgentCitation.builder()
                .citationId(citation.getCitationId())
                .title(AgentSensitiveDataPolicy.redactText(citation.getTitle()))
                .url(safeUrl)
                .publisher(AgentSensitiveDataPolicy.redactText(citation.getPublisher()))
                .publicationDate(citation.getPublicationDate())
                .retrievedAt(citation.getRetrievedAt())
                .dataDate(citation.getDataDate())
                .excerpt(AgentSensitiveDataPolicy.redactText(citation.getExcerpt()))
                .freshness(citation.getFreshness())
                .sourceType(citation.getSourceType())
                .availability(safeUrl == null ? "UNLINKED" : "AVAILABLE")
                .conflict(citation.isConflict())
                .build();
    }

    private String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
