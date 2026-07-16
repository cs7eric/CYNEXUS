package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;

public interface MarketInformationAiTaskRepository {
    int recoverStaleTasks(int timeoutMinutes);
    boolean tryReserveProviderRequest(String providerCode, String usageScope, int limit);
    MarketInformationAiTaskBO claimNext(String workerId);
    MarketInformationAiTaskBO loadInput(MarketInformationAiTaskBO task);
    String loadBriefInput(String eventSchemaVersion, String eventPromptVersion);
    void saveEventAnalysis(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion,
                           String providerCode, String modelCode, String analysisJson,
                           String responsePreview, boolean fallbackUsed);
    void saveDailyBrief(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion,
                        String providerCode, String modelCode, String briefJson, boolean fallbackUsed);
    void markSucceeded(long taskId);
    void markFailed(MarketInformationAiTaskBO task, String errorMessage);
    void enqueueDailyBrief(String eventSchemaVersion, String eventPromptVersion);
}
