package cn.cccs7.cynexus.ai.domain.service.configuration;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import java.util.List;

/** Domain entry point for AI configuration lifecycle and cross-reference rules. */
public interface AiConfigurationDomainService {

    List<AgentQuotaPolicyBO> listAgentQuotaPolicies();
    AgentQuotaPolicyBO findAgentQuotaPolicy(Long id);
    Long createAgentQuotaPolicy(AgentQuotaPolicyBO policy);
    boolean updateAgentQuotaPolicy(AgentQuotaPolicyBO policy);
    boolean deleteAgentQuotaPolicy(Long id);

    List<AiProviderBO> listProviders();
    AiProviderBO findProviderById(Long id);
    Long createProvider(AiProviderBO provider);
    boolean updateProvider(AiProviderBO provider);
    boolean deleteProvider(Long id);

    List<AiPromptBO> listPrompts(String keyword, String scenarioCode, String status);
    AiPromptBO findPromptById(Long id);
    Long createPrompt(AiPromptBO prompt);
    boolean updatePrompt(AiPromptBO prompt);
    boolean deletePrompt(Long id);

    List<AiUsageBindingBO> listUsageBindings();
    AiUsageBindingBO findUsageBindingById(Long id);
    Long createUsageBinding(AiUsageBindingBO binding);
    boolean updateUsageBinding(AiUsageBindingBO binding);
    boolean deleteUsageBinding(Long id);

    List<ApplicationAnalysisConfigBO> listAnalysisConfigs();
    ApplicationAnalysisConfigBO findAnalysisConfigById(Long id);
    Long createAnalysisConfig(ApplicationAnalysisConfigBO config);
    boolean updateAnalysisConfig(ApplicationAnalysisConfigBO config);
    boolean deleteAnalysisConfig(Long id);

    List<ApplicationAnalysisDailySnapshotBO> listAnalysisDailySnapshots();
    ApplicationAnalysisDailySnapshotBO findAnalysisDailySnapshotById(Long id);
    Long createAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot);
    boolean updateAnalysisDailySnapshot(ApplicationAnalysisDailySnapshotBO snapshot);
    boolean deleteAnalysisDailySnapshot(Long id);

    List<ApplicationAnalysisResultCurrentBO> listAnalysisResultCurrent();
    ApplicationAnalysisResultCurrentBO findAnalysisResultCurrentById(Long id);
    Long createAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result);
    boolean updateAnalysisResultCurrent(ApplicationAnalysisResultCurrentBO result);
    boolean deleteAnalysisResultCurrent(Long id);

    List<ApplicationAnalysisResultHistoryBO> listAnalysisResultHistory();
    ApplicationAnalysisResultHistoryBO findAnalysisResultHistoryById(Long id);
    Long createAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result);
    boolean updateAnalysisResultHistory(ApplicationAnalysisResultHistoryBO result);
    boolean deleteAnalysisResultHistory(Long id);

    List<ApplicationAnalysisTargetBO> listAnalysisTargets();
    ApplicationAnalysisTargetBO findAnalysisTargetById(Long id);
    Long createAnalysisTarget(ApplicationAnalysisTargetBO target);
    boolean updateAnalysisTarget(ApplicationAnalysisTargetBO target);
    boolean deleteAnalysisTarget(Long id);
}
