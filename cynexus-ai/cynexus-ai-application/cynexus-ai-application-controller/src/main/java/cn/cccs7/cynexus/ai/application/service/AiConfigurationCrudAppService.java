package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisConfigRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisDailySnapshotRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultCurrentRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultHistoryRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisTargetRepository;
import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.domain.policy.quota.AgentQuotaPolicy;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiConfigurationCrudAppService {

    private final AiProviderRepository aiProviderRepository;
    private final AiPromptRepository aiPromptRepository;
    private final AiUsageBindingRepository aiUsageBindingRepository;
    private final ApplicationAnalysisConfigRepository applicationAnalysisConfigRepository;
    private final ApplicationAnalysisDailySnapshotRepository applicationAnalysisDailySnapshotRepository;
    private final ApplicationAnalysisResultCurrentRepository applicationAnalysisResultCurrentRepository;
    private final ApplicationAnalysisResultHistoryRepository applicationAnalysisResultHistoryRepository;
    private final ApplicationAnalysisTargetRepository applicationAnalysisTargetRepository;
    private final AgentQuotaPolicyRepository agentQuotaPolicyRepository;

    public AiConfigurationCrudAppService(
            AiProviderRepository aiProviderRepository,
            AiPromptRepository aiPromptRepository,
            AiUsageBindingRepository aiUsageBindingRepository,
            ApplicationAnalysisConfigRepository applicationAnalysisConfigRepository,
            ApplicationAnalysisDailySnapshotRepository applicationAnalysisDailySnapshotRepository,
            ApplicationAnalysisResultCurrentRepository applicationAnalysisResultCurrentRepository,
            ApplicationAnalysisResultHistoryRepository applicationAnalysisResultHistoryRepository,
            ApplicationAnalysisTargetRepository applicationAnalysisTargetRepository,
            AgentQuotaPolicyRepository agentQuotaPolicyRepository) {
        this.aiProviderRepository = aiProviderRepository;
        this.aiPromptRepository = aiPromptRepository;
        this.aiUsageBindingRepository = aiUsageBindingRepository;
        this.applicationAnalysisConfigRepository = applicationAnalysisConfigRepository;
        this.applicationAnalysisDailySnapshotRepository = applicationAnalysisDailySnapshotRepository;
        this.applicationAnalysisResultCurrentRepository = applicationAnalysisResultCurrentRepository;
        this.applicationAnalysisResultHistoryRepository = applicationAnalysisResultHistoryRepository;
        this.applicationAnalysisTargetRepository = applicationAnalysisTargetRepository;
        this.agentQuotaPolicyRepository = agentQuotaPolicyRepository;
    }

    public List<AgentQuotaPolicyBO> listAgentQuotaPolicies() { return agentQuotaPolicyRepository.listAll(); }
    public AgentQuotaPolicyBO findAgentQuotaPolicy(Long id) { return agentQuotaPolicyRepository.findById(id); }
    public Long createAgentQuotaPolicy(AgentQuotaPolicyBO policy) { AgentQuotaPolicy.validate(policy); return agentQuotaPolicyRepository.create(policy); }
    public boolean updateAgentQuotaPolicy(AgentQuotaPolicyBO policy) { AgentQuotaPolicy.validate(policy); return agentQuotaPolicyRepository.update(policy); }
    public boolean deleteAgentQuotaPolicy(Long id) { return agentQuotaPolicyRepository.delete(id); }

    public List<AiProviderBO> listProviders() {
        return aiProviderRepository.listAll();
    }

    public AiProviderBO findProviderById(Long id) {
        return aiProviderRepository.findById(id);
    }

    public Long createProvider(AiProviderBO provider) {
        return aiProviderRepository.create(provider);
    }

    public boolean updateProvider(AiProviderBO provider) {
        return aiProviderRepository.update(provider);
    }

    public boolean deleteProvider(Long id) {
        return aiProviderRepository.delete(id);
    }

    public List<AiPromptBO> listPrompts(String keyword, String scenarioCode, String status) {
        return aiPromptRepository.list(keyword, scenarioCode, status);
    }

    public AiPromptBO findPromptById(Long id) {
        return aiPromptRepository.findById(id);
    }

    public Long createPrompt(AiPromptBO prompt) {
        return aiPromptRepository.create(prompt);
    }

    public boolean updatePrompt(AiPromptBO prompt) {
        return aiPromptRepository.update(prompt);
    }

    public boolean deletePrompt(Long id) {
        return aiPromptRepository.delete(id);
    }

    public List<AiUsageBindingBO> listUsageBindings() {
        return aiUsageBindingRepository.listAll();
    }

    public AiUsageBindingBO findUsageBindingById(Long id) {
        return aiUsageBindingRepository.findById(id);
    }

    public Long createUsageBinding(AiUsageBindingBO binding) {
        validateBindingReferences(binding);
        return aiUsageBindingRepository.create(binding);
    }

    public boolean updateUsageBinding(AiUsageBindingBO binding) {
        validateBindingReferences(binding);
        return aiUsageBindingRepository.update(binding);
    }

    public boolean deleteUsageBinding(Long id) {
        return aiUsageBindingRepository.delete(id);
    }

    private void validateBindingReferences(AiUsageBindingBO binding) {
        if (binding == null) {
            throw new IllegalArgumentException("AI usage binding is required");
        }
        if (binding.getProviderId() != null && aiProviderRepository.findById(binding.getProviderId()) == null) {
            throw new IllegalArgumentException("AI provider binding target does not exist");
        }
        if (binding.getPromptId() != null) {
            AiPromptBO prompt = aiPromptRepository.findById(binding.getPromptId());
            if (prompt == null || !"active".equalsIgnoreCase(prompt.getStatus())) {
                throw new IllegalArgumentException("AI prompt binding target must be active");
            }
        }
    }

    public List<ApplicationAnalysisConfigBO> listAnalysisConfigs() {
        return applicationAnalysisConfigRepository.listAll();
    }

    public ApplicationAnalysisConfigBO findAnalysisConfigById(Long id) {
        return applicationAnalysisConfigRepository.findById(id);
    }

    public Long createAnalysisConfig(ApplicationAnalysisConfigBO config) {
        return applicationAnalysisConfigRepository.create(config);
    }

    public boolean updateAnalysisConfig(ApplicationAnalysisConfigBO config) {
        return applicationAnalysisConfigRepository.update(config);
    }

    public boolean deleteAnalysisConfig(Long id) {
        return applicationAnalysisConfigRepository.delete(id);
    }

    public List<ApplicationAnalysisDailySnapshotBO> listAnalysisDailySnapshots() {
        return applicationAnalysisDailySnapshotRepository.listAll();
    }

    public ApplicationAnalysisDailySnapshotBO findAnalysisDailySnapshotById(Long id) {
        return applicationAnalysisDailySnapshotRepository.findById(id);
    }

    public Long createAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) {
        return applicationAnalysisDailySnapshotRepository.create(snapshot);
    }

    public boolean updateAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) {
        return applicationAnalysisDailySnapshotRepository.update(snapshot);
    }

    public boolean deleteAnalysisDailySnapshot(Long id) {
        return applicationAnalysisDailySnapshotRepository.delete(id);
    }

    public List<ApplicationAnalysisResultCurrentBO> listAnalysisResultCurrent() {
        return applicationAnalysisResultCurrentRepository.listAll();
    }

    public ApplicationAnalysisResultCurrentBO findAnalysisResultCurrentById(Long id) {
        return applicationAnalysisResultCurrentRepository.findById(id);
    }

    public Long createAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) {
        return applicationAnalysisResultCurrentRepository.create(result);
    }

    public boolean updateAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) {
        return applicationAnalysisResultCurrentRepository.update(result);
    }

    public boolean deleteAnalysisResultCurrent(Long id) {
        return applicationAnalysisResultCurrentRepository.delete(id);
    }

    public List<ApplicationAnalysisResultHistoryBO> listAnalysisResultHistory() {
        return applicationAnalysisResultHistoryRepository.listAll();
    }

    public ApplicationAnalysisResultHistoryBO findAnalysisResultHistoryById(Long id) {
        return applicationAnalysisResultHistoryRepository.findById(id);
    }

    public Long createAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) {
        return applicationAnalysisResultHistoryRepository.create(result);
    }

    public boolean updateAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) {
        return applicationAnalysisResultHistoryRepository.update(result);
    }

    public boolean deleteAnalysisResultHistory(Long id) {
        return applicationAnalysisResultHistoryRepository.delete(id);
    }

    public List<ApplicationAnalysisTargetBO> listAnalysisTargets() {
        return applicationAnalysisTargetRepository.listAll();
    }

    public ApplicationAnalysisTargetBO findAnalysisTargetById(Long id) {
        return applicationAnalysisTargetRepository.findById(id);
    }

    public Long createAnalysisTarget(ApplicationAnalysisTargetBO target) {
        return applicationAnalysisTargetRepository.create(target);
    }

    public boolean updateAnalysisTarget(ApplicationAnalysisTargetBO target) {
        return applicationAnalysisTargetRepository.update(target);
    }

    public boolean deleteAnalysisTarget(Long id) {
        return applicationAnalysisTargetRepository.delete(id);
    }
}
