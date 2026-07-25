package cn.cccs7.cynexus.ai.domain.service.configuration;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.ai.domain.policy.quota.AgentQuotaPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisConfigRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisDailySnapshotRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultCurrentRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultHistoryRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisTargetRepository;
import java.util.List;

/**
 * Coordinates configuration aggregates and keeps reference/state rules in the domain.
 * Repository interfaces are domain ports; persistence details stay in infrastructure.
 */
public class AiConfigurationDomainServiceImpl implements AiConfigurationDomainService {

    private final AiProviderRepository providerRepository;
    private final AiPromptRepository promptRepository;
    private final AiUsageBindingRepository usageBindingRepository;
    private final ApplicationAnalysisConfigRepository analysisConfigRepository;
    private final ApplicationAnalysisDailySnapshotRepository dailySnapshotRepository;
    private final ApplicationAnalysisResultCurrentRepository resultCurrentRepository;
    private final ApplicationAnalysisResultHistoryRepository resultHistoryRepository;
    private final ApplicationAnalysisTargetRepository targetRepository;
    private final AgentQuotaPolicyRepository quotaPolicyRepository;

    public AiConfigurationDomainServiceImpl(
            AiProviderRepository providerRepository,
            AiPromptRepository promptRepository,
            AiUsageBindingRepository usageBindingRepository,
            ApplicationAnalysisConfigRepository analysisConfigRepository,
            ApplicationAnalysisDailySnapshotRepository dailySnapshotRepository,
            ApplicationAnalysisResultCurrentRepository resultCurrentRepository,
            ApplicationAnalysisResultHistoryRepository resultHistoryRepository,
            ApplicationAnalysisTargetRepository targetRepository,
            AgentQuotaPolicyRepository quotaPolicyRepository) {
        this.providerRepository = providerRepository;
        this.promptRepository = promptRepository;
        this.usageBindingRepository = usageBindingRepository;
        this.analysisConfigRepository = analysisConfigRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.resultCurrentRepository = resultCurrentRepository;
        this.resultHistoryRepository = resultHistoryRepository;
        this.targetRepository = targetRepository;
        this.quotaPolicyRepository = quotaPolicyRepository;
    }

    @Override public List<AgentQuotaPolicyBO> listAgentQuotaPolicies() { return quotaPolicyRepository.listAll(); }
    @Override public AgentQuotaPolicyBO findAgentQuotaPolicy(Long id) { return quotaPolicyRepository.findById(id); }
    @Override public Long createAgentQuotaPolicy(AgentQuotaPolicyBO policy) {
        AgentQuotaPolicy.validate(policy);
        return quotaPolicyRepository.create(policy);
    }
    @Override public boolean updateAgentQuotaPolicy(AgentQuotaPolicyBO policy) {
        AgentQuotaPolicy.validate(policy);
        return quotaPolicyRepository.update(policy);
    }
    @Override public boolean deleteAgentQuotaPolicy(Long id) { return quotaPolicyRepository.delete(id); }

    @Override public List<AiProviderBO> listProviders() { return providerRepository.listAll(); }
    @Override public AiProviderBO findProviderById(Long id) { return providerRepository.findById(id); }
    @Override public Long createProvider(AiProviderBO provider) {
        validateProvider(provider);
        return providerRepository.create(provider);
    }
    @Override public boolean updateProvider(AiProviderBO provider) {
        validateProvider(provider);
        if (Boolean.FALSE.equals(provider.getEnabled()) && hasEnabledBindingForProvider(provider.getId())) {
            throw new IllegalStateException("Cannot disable a provider referenced by an enabled AI usage binding");
        }
        return providerRepository.update(provider);
    }
    @Override public boolean deleteProvider(Long id) {
        requireId(id, "AI provider id");
        if (hasBindingForProvider(id)) {
            throw new IllegalStateException("Cannot delete an AI provider referenced by an AI usage binding");
        }
        return providerRepository.delete(id);
    }

    @Override public List<AiPromptBO> listPrompts(String keyword, String scenarioCode, String status) {
        if (status != null && !status.isBlank()) validateStatus(status, "prompt status");
        return promptRepository.list(keyword, scenarioCode, status);
    }
    @Override public AiPromptBO findPromptById(Long id) { return promptRepository.findById(id); }
    @Override public Long createPrompt(AiPromptBO prompt) {
        normalizePromptDefaults(prompt);
        validatePrompt(prompt);
        return promptRepository.create(prompt);
    }
    @Override public boolean updatePrompt(AiPromptBO prompt) {
        validatePrompt(prompt);
        if (!AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus()) && hasEnabledBindingForPrompt(prompt.getId())) {
            throw new IllegalStateException("Cannot archive a prompt referenced by an enabled AI usage binding");
        }
        return promptRepository.update(prompt);
    }
    @Override public boolean deletePrompt(Long id) {
        requireId(id, "AI prompt id");
        if (hasBindingForPrompt(id)) {
            throw new IllegalStateException("Cannot delete an AI prompt referenced by an AI usage binding");
        }
        return promptRepository.delete(id);
    }

    @Override public List<AiUsageBindingBO> listUsageBindings() { return usageBindingRepository.listAll(); }
    @Override public AiUsageBindingBO findUsageBindingById(Long id) { return usageBindingRepository.findById(id); }
    @Override public Long createUsageBinding(AiUsageBindingBO binding) {
        validateUsageBinding(binding);
        return usageBindingRepository.create(binding);
    }
    @Override public boolean updateUsageBinding(AiUsageBindingBO binding) {
        validateUsageBinding(binding);
        return usageBindingRepository.update(binding);
    }
    @Override public boolean deleteUsageBinding(Long id) { return usageBindingRepository.delete(id); }

    @Override public List<ApplicationAnalysisConfigBO> listAnalysisConfigs() { return analysisConfigRepository.listAll(); }
    @Override public ApplicationAnalysisConfigBO findAnalysisConfigById(Long id) { return analysisConfigRepository.findById(id); }
    @Override public Long createAnalysisConfig(ApplicationAnalysisConfigBO config) {
        validateAnalysisConfig(config);
        return analysisConfigRepository.create(config);
    }
    @Override public boolean updateAnalysisConfig(ApplicationAnalysisConfigBO config) {
        validateAnalysisConfig(config);
        return analysisConfigRepository.update(config);
    }
    @Override public boolean deleteAnalysisConfig(Long id) { return analysisConfigRepository.delete(id); }

    @Override public List<ApplicationAnalysisDailySnapshotBO> listAnalysisDailySnapshots() { return dailySnapshotRepository.listAll(); }
    @Override public ApplicationAnalysisDailySnapshotBO findAnalysisDailySnapshotById(Long id) { return dailySnapshotRepository.findById(id); }
    @Override public Long createAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) { return dailySnapshotRepository.create(snapshot); }
    @Override public boolean updateAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) { return dailySnapshotRepository.update(snapshot); }
    @Override public boolean deleteAnalysisDailySnapshot(Long id) { return dailySnapshotRepository.delete(id); }

    @Override public List<ApplicationAnalysisResultCurrentBO> listAnalysisResultCurrent() { return resultCurrentRepository.listAll(); }
    @Override public ApplicationAnalysisResultCurrentBO findAnalysisResultCurrentById(Long id) { return resultCurrentRepository.findById(id); }
    @Override public Long createAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) { return resultCurrentRepository.create(result); }
    @Override public boolean updateAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) { return resultCurrentRepository.update(result); }
    @Override public boolean deleteAnalysisResultCurrent(Long id) { return resultCurrentRepository.delete(id); }

    @Override public List<ApplicationAnalysisResultHistoryBO> listAnalysisResultHistory() { return resultHistoryRepository.listAll(); }
    @Override public ApplicationAnalysisResultHistoryBO findAnalysisResultHistoryById(Long id) { return resultHistoryRepository.findById(id); }
    @Override public Long createAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) { return resultHistoryRepository.create(result); }
    @Override public boolean updateAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) { return resultHistoryRepository.update(result); }
    @Override public boolean deleteAnalysisResultHistory(Long id) { return resultHistoryRepository.delete(id); }

    @Override public List<ApplicationAnalysisTargetBO> listAnalysisTargets() { return targetRepository.listAll(); }
    @Override public ApplicationAnalysisTargetBO findAnalysisTargetById(Long id) { return targetRepository.findById(id); }
    @Override public Long createAnalysisTarget(ApplicationAnalysisTargetBO target) { return targetRepository.create(target); }
    @Override public boolean updateAnalysisTarget(ApplicationAnalysisTargetBO target) { return targetRepository.update(target); }
    @Override public boolean deleteAnalysisTarget(Long id) { return targetRepository.delete(id); }

    private void validateProvider(AiProviderBO provider) {
        require(provider, "AI provider is required");
        requireText(provider.getCode(), "AI provider code is required");
        requireText(provider.getName(), "AI provider name is required");
        requireText(provider.getProviderType(), "AI provider type is required");
        if (provider.getTimeoutSeconds() != null && provider.getTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("AI provider timeout must be positive");
        }
        if (provider.getInputPricePerMillion() != null && provider.getInputPricePerMillion().signum() < 0
                || provider.getOutputPricePerMillion() != null && provider.getOutputPricePerMillion().signum() < 0) {
            throw new IllegalArgumentException("AI provider token prices cannot be negative");
        }
    }

    private void validatePrompt(AiPromptBO prompt) {
        require(prompt, "AI prompt is required");
        requireText(prompt.getPromptKey(), "AI prompt key is required");
        requireText(prompt.getName(), "AI prompt name is required");
        requireText(prompt.getScenarioCode(), "AI prompt scenario is required");
        requireText(prompt.getPromptVersion(), "AI prompt version is required");
        requireText(prompt.getStatus(), "AI prompt status is required");
        validateStatus(prompt.getStatus(), "prompt status");
    }

    private void validateUsageBinding(AiUsageBindingBO binding) {
        require(binding, "AI usage binding is required");
        requireText(binding.getCapability(), "AI usage binding capability is required");
        if (binding.getProviderId() != null) {
            AiProviderBO provider = providerRepository.findById(binding.getProviderId());
            if (provider == null) throw new IllegalArgumentException("AI provider binding target does not exist");
            if (Boolean.TRUE.equals(binding.getEnabled()) && Boolean.FALSE.equals(provider.getEnabled())) {
                throw new IllegalArgumentException("Enabled AI usage binding requires an enabled provider");
            }
        }
        if (binding.getPromptId() != null) {
            AiPromptBO prompt = promptRepository.findById(binding.getPromptId());
            if (prompt == null) throw new IllegalArgumentException("AI prompt binding target does not exist");
            if (Boolean.TRUE.equals(binding.getEnabled()) && !AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus())) {
                throw new IllegalArgumentException("Enabled AI usage binding requires an active prompt");
            }
        }
    }

    private void validateAnalysisConfig(ApplicationAnalysisConfigBO config) {
        require(config, "Application analysis config is required");
        requireText(config.getConfigKey(), "Application analysis config key is required");
        requireText(config.getStatus(), "Application analysis config status is required");
        validateStatus(config.getStatus(), "analysis config status");
        requirePositive(config.getHorizonDays(), "Application analysis horizon days must be positive");
        requirePositive(config.getHorizonSegments(), "Application analysis horizon segments must be positive");
        requireNonNegative(config.getMonthlyKeep(), "Application analysis monthly keep cannot be negative");
        requireNonNegative(config.getWeeklyKeep(), "Application analysis weekly keep cannot be negative");
    }

    private void validateStatus(String status, String field) {
        if (!AiConstants.STATUS_DRAFT.equalsIgnoreCase(status) && !AiConstants.STATUS_ACTIVE.equalsIgnoreCase(status)
                && !AiConstants.STATUS_ARCHIVED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + status);
        }
    }

    private void normalizePromptDefaults(AiPromptBO prompt) {
        require(prompt, "AI prompt is required");
        if (prompt.getPromptVersion() == null || prompt.getPromptVersion().isBlank()) prompt.setPromptVersion("v1");
        if (prompt.getStatus() == null || prompt.getStatus().isBlank()) prompt.setStatus(AiConstants.STATUS_DRAFT);
    }

    private boolean hasBindingForProvider(Long providerId) {
        return usageBindingRepository.listAll().stream()
                .anyMatch(binding -> java.util.Objects.equals(providerId, binding.getProviderId()));
    }

    private boolean hasEnabledBindingForProvider(Long providerId) {
        return usageBindingRepository.listAll().stream()
                .anyMatch(binding -> Boolean.TRUE.equals(binding.getEnabled())
                        && java.util.Objects.equals(providerId, binding.getProviderId()));
    }

    private boolean hasBindingForPrompt(Long promptId) {
        return usageBindingRepository.listAll().stream()
                .anyMatch(binding -> java.util.Objects.equals(promptId, binding.getPromptId()));
    }

    private boolean hasEnabledBindingForPrompt(Long promptId) {
        return usageBindingRepository.listAll().stream()
                .anyMatch(binding -> Boolean.TRUE.equals(binding.getEnabled())
                        && java.util.Objects.equals(promptId, binding.getPromptId()));
    }

    private void require(Object value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private void requirePositive(Integer value, String message) {
        if (value != null && value <= 0) throw new IllegalArgumentException(message);
    }

    private void requireNonNegative(Integer value, String message) {
        if (value != null && value < 0) throw new IllegalArgumentException(message);
    }

    private void requireId(Long value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
    }
}
