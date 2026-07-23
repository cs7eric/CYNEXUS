package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentCapabilityDescriptorPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.repository.capability.AgentCapabilityRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Agent run aggregate orchestration. Application code supplies API mapping and
 * provider/tool ports; this service owns persistence, routing, and run-state transitions.
 */
@Service
public class AgentRunDomainService {
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

    public void saveStep(AgentRunStepBO step) {
        runRepository.saveStep(step);
    }

    public void saveMessage(AgentMessageBO message) {
        runRepository.saveMessage(message);
    }

    public List<AgentMessageBO> findMessages(String ownerKey, String conversationNo) {
        return runRepository.findMessages(ownerKey, conversationNo);
    }

    public List<AgentRunStepBO> findSteps(String ownerKey, String runNo) {
        return runRepository.findSteps(ownerKey, runNo);
    }

    public void saveEvent(String ownerKey, AgentRunEventBO event) {
        runRepository.saveEvent(ownerKey, event);
    }

    public Optional<AgentRunBO> findRun(String ownerKey, String runNo) {
        return runRepository.findRun(ownerKey, runNo);
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
        List<AiUsageBindingBO> bindings = usageBindingRepository.listAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled())).toList();
        AiUsageBindingBO binding = firstBinding(bindings, "autonomous_agent")
                .or(() -> firstBinding(bindings, "workspace_chat"))
                .or(() -> firstBinding(bindings, "text_summary"))
                .orElseThrow(() -> new IllegalStateException("No AI provider is bound to autonomous_agent"));
        AiProviderBO provider = providerRepository.findById(binding.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("The AI provider bound to autonomous_agent is unavailable");
        }
        String modelCode = preferredModelCode == null || preferredModelCode.isBlank()
                ? (binding.getModelOverride() == null || binding.getModelOverride().isBlank()
                ? provider.getDefaultModel() : binding.getModelOverride()) : preferredModelCode;
        return AiModelBO.builder().modelCode(modelCode).modelName(modelCode)
                .providerCode(provider.getProviderType() == null || provider.getProviderType().isBlank()
                        ? provider.getCode() : provider.getProviderType())
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra()).enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).toolCallSupported(true).build()).build();
    }

    private Optional<AiUsageBindingBO> firstBinding(List<AiUsageBindingBO> bindings, String capability) {
        return bindings.stream().filter(item -> capability.equals(item.getCapability())).findFirst();
    }
}
