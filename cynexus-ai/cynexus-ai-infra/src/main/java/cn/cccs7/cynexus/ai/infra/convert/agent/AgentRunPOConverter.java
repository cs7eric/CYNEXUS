package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
public final class AgentRunPOConverter {
    public static final AgentRunPOConverter INSTANCE = new AgentRunPOConverter();

    private AgentRunPOConverter() {
    }

    public AgentRunPO toSource(AgentRunBO source) {
        if (source == null) return null;
        AgentRunPO target = new AgentRunPO();
        target.setId(source.getId()); target.setRunNo(source.getRunNo()); target.setConversationNo(source.getConversationNo());
        target.setOwnerKey(source.getOwnerKey()); target.setIdempotencyKey(source.getIdempotencyKey()); target.setStatus(source.getStatus()); target.setUserInput(source.getUserInput());
        target.setAnswer(source.getAnswer()); target.setProviderCode(source.getProviderCode()); target.setModelCode(source.getModelCode()); target.setReasoningEffort(source.getReasoningEffort()); target.setPlan(source.getPlanJson());
        target.setContextJson(source.getContextJson()); target.setAssumptionsJson(source.getAssumptionsJson());
        target.setEvidenceJson(source.getEvidenceJson()); target.setWarningsJson(source.getWarningsJson());
        target.setErrorMessage(source.getErrorMessage()); target.setFailureCategory(source.getFailureCategory()); target.setTurnCount(source.getTurnCount());
        target.setToolCallCount(source.getToolCallCount()); target.setStartedAt(source.getStartedAt());
        target.setInputTokens(source.getInputTokens()); target.setCachedInputTokens(source.getCachedInputTokens());
        target.setCacheWriteTokens(source.getCacheWriteTokens()); target.setOutputTokens(source.getOutputTokens()); target.setTotalTokens(source.getTotalTokens());
        target.setEstimatedCost(source.getEstimatedCost()); target.setUsageSource(source.getUsageSource());
        target.setFinishedAt(source.getFinishedAt()); target.setCreatedAt(source.getCreatedAt());
        target.setCancellationRequestedAt(source.getCancellationRequestedAt());
        target.setCancellationRequestedBy(source.getCancellationRequestedBy());
        target.setUpdatedAt(source.getUpdatedAt()); target.setDeletedAt(source.getDeletedAt());
        return target;
    }

    public AgentRunBO toTarget(AgentRunPO source) {
        if (source == null) return null;
        return AgentRunBO.builder().id(source.getId()).runNo(source.getRunNo()).conversationNo(source.getConversationNo())
                .ownerKey(source.getOwnerKey()).idempotencyKey(source.getIdempotencyKey()).status(source.getStatus()).userInput(source.getUserInput())
                .answer(source.getAnswer()).providerCode(source.getProviderCode()).modelCode(source.getModelCode()).reasoningEffort(source.getReasoningEffort()).planJson(source.getPlan())
                .contextJson(source.getContextJson()).assumptionsJson(source.getAssumptionsJson())
                .evidenceJson(source.getEvidenceJson()).warningsJson(source.getWarningsJson())
                .errorMessage(source.getErrorMessage()).failureCategory(source.getFailureCategory()).turnCount(source.getTurnCount()).toolCallCount(source.getToolCallCount())
                .inputTokens(source.getInputTokens()).cachedInputTokens(source.getCachedInputTokens())
                .cacheWriteTokens(source.getCacheWriteTokens()).outputTokens(source.getOutputTokens()).totalTokens(source.getTotalTokens())
                .estimatedCost(source.getEstimatedCost()).usageSource(source.getUsageSource())
                .startedAt(source.getStartedAt()).finishedAt(source.getFinishedAt()).createdAt(source.getCreatedAt())
                .cancellationRequestedAt(source.getCancellationRequestedAt())
                .cancellationRequestedBy(source.getCancellationRequestedBy())
                .updatedAt(source.getUpdatedAt()).deletedAt(source.getDeletedAt()).build();
    }
}
