package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Application adapter for the AI configuration HTTP use cases. */
@Service
public class AiConfigurationCrudAppService {

    private final AiConfigurationDomainService domainService;

    public AiConfigurationCrudAppService(AiConfigurationDomainService domainService) {
        this.domainService = domainService;
    }

    public List<AgentQuotaPolicyBO> listAgentQuotaPolicies() { return domainService.listAgentQuotaPolicies(); }
    public AgentQuotaPolicyBO findAgentQuotaPolicy(Long id) { return domainService.findAgentQuotaPolicy(id); }
    public Long createAgentQuotaPolicy(AgentQuotaPolicyBO policy) { return domainService.createAgentQuotaPolicy(policy); }
    public boolean updateAgentQuotaPolicy(AgentQuotaPolicyBO policy) { return domainService.updateAgentQuotaPolicy(policy); }
    public boolean deleteAgentQuotaPolicy(Long id) { return domainService.deleteAgentQuotaPolicy(id); }

    public List<AiProviderBO> listProviders() { return domainService.listProviders(); }
    public AiProviderBO findProviderById(Long id) { return domainService.findProviderById(id); }
    public Long createProvider(AiProviderBO provider) { return domainService.createProvider(provider); }
    public boolean updateProvider(AiProviderBO provider) { return domainService.updateProvider(provider); }
    public boolean deleteProvider(Long id) { return domainService.deleteProvider(id); }

    public List<AiPromptBO> listPrompts(String keyword, String scenarioCode, String status) {
        return domainService.listPrompts(keyword, scenarioCode, status);
    }
    public AiPromptBO findPromptById(Long id) { return domainService.findPromptById(id); }
    public Long createPrompt(AiPromptBO prompt) { return domainService.createPrompt(prompt); }
    public boolean updatePrompt(AiPromptBO prompt) { return domainService.updatePrompt(prompt); }
    public boolean deletePrompt(Long id) { return domainService.deletePrompt(id); }

    public List<AiUsageBindingBO> listUsageBindings() { return domainService.listUsageBindings(); }
    public AiUsageBindingBO findUsageBindingById(Long id) { return domainService.findUsageBindingById(id); }
    public Long createUsageBinding(AiUsageBindingBO binding) { return domainService.createUsageBinding(binding); }
    public boolean updateUsageBinding(AiUsageBindingBO binding) { return domainService.updateUsageBinding(binding); }
    public boolean deleteUsageBinding(Long id) { return domainService.deleteUsageBinding(id); }

    public List<ApplicationAnalysisConfigBO> listAnalysisConfigs() { return domainService.listAnalysisConfigs(); }
    public ApplicationAnalysisConfigBO findAnalysisConfigById(Long id) { return domainService.findAnalysisConfigById(id); }
    public Long createAnalysisConfig(ApplicationAnalysisConfigBO config) { return domainService.createAnalysisConfig(config); }
    public boolean updateAnalysisConfig(ApplicationAnalysisConfigBO config) { return domainService.updateAnalysisConfig(config); }
    public boolean deleteAnalysisConfig(Long id) { return domainService.deleteAnalysisConfig(id); }

    public List<ApplicationAnalysisDailySnapshotBO> listAnalysisDailySnapshots() {
        return domainService.listAnalysisDailySnapshots();
    }
    public ApplicationAnalysisDailySnapshotBO findAnalysisDailySnapshotById(Long id) {
        return domainService.findAnalysisDailySnapshotById(id);
    }
    public Long createAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) {
        return domainService.createAnalysisDailySnapshot(snapshot);
    }
    public boolean updateAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot) {
        return domainService.updateAnalysisDailySnapshot(snapshot);
    }
    public boolean deleteAnalysisDailySnapshot(Long id) { return domainService.deleteAnalysisDailySnapshot(id); }

    public List<ApplicationAnalysisResultCurrentBO> listAnalysisResultCurrent() {
        return domainService.listAnalysisResultCurrent();
    }
    public ApplicationAnalysisResultCurrentBO findAnalysisResultCurrentById(Long id) {
        return domainService.findAnalysisResultCurrentById(id);
    }
    public Long createAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) {
        return domainService.createAnalysisResultCurrent(result);
    }
    public boolean updateAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result) {
        return domainService.updateAnalysisResultCurrent(result);
    }
    public boolean deleteAnalysisResultCurrent(Long id) { return domainService.deleteAnalysisResultCurrent(id); }

    public List<ApplicationAnalysisResultHistoryBO> listAnalysisResultHistory() {
        return domainService.listAnalysisResultHistory();
    }
    public ApplicationAnalysisResultHistoryBO findAnalysisResultHistoryById(Long id) {
        return domainService.findAnalysisResultHistoryById(id);
    }
    public Long createAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) {
        return domainService.createAnalysisResultHistory(result);
    }
    public boolean updateAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result) {
        return domainService.updateAnalysisResultHistory(result);
    }
    public boolean deleteAnalysisResultHistory(Long id) { return domainService.deleteAnalysisResultHistory(id); }

    public List<ApplicationAnalysisTargetBO> listAnalysisTargets() { return domainService.listAnalysisTargets(); }
    public ApplicationAnalysisTargetBO findAnalysisTargetById(Long id) { return domainService.findAnalysisTargetById(id); }
    public Long createAnalysisTarget(ApplicationAnalysisTargetBO target) { return domainService.createAnalysisTarget(target); }
    public boolean updateAnalysisTarget(ApplicationAnalysisTargetBO target) { return domainService.updateAnalysisTarget(target); }
    public boolean deleteAnalysisTarget(Long id) { return domainService.deleteAnalysisTarget(id); }
}
