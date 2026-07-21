package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunPO;
public final class AgentRunPOConverter {
    public static final AgentRunPOConverter INSTANCE = new AgentRunPOConverter();

    private AgentRunPOConverter() {
    }

    public AgentRunPO toSource(AgentRunBO source) {
        if (source == null) return null;
        AgentRunPO target = new AgentRunPO();
        target.setId(source.getId()); target.setRunNo(source.getRunNo()); target.setConversationNo(source.getConversationNo());
        target.setOwnerKey(source.getOwnerKey()); target.setStatus(source.getStatus()); target.setUserInput(source.getUserInput());
        target.setAnswer(source.getAnswer()); target.setModelCode(source.getModelCode()); target.setPlan(source.getPlanJson());
        target.setErrorMessage(source.getErrorMessage()); target.setTurnCount(source.getTurnCount());
        target.setToolCallCount(source.getToolCallCount()); target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt()); target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt()); target.setDeletedAt(source.getDeletedAt());
        return target;
    }

    public AgentRunBO toTarget(AgentRunPO source) {
        if (source == null) return null;
        return AgentRunBO.builder().id(source.getId()).runNo(source.getRunNo()).conversationNo(source.getConversationNo())
                .ownerKey(source.getOwnerKey()).status(source.getStatus()).userInput(source.getUserInput())
                .answer(source.getAnswer()).modelCode(source.getModelCode()).planJson(source.getPlan())
                .errorMessage(source.getErrorMessage()).turnCount(source.getTurnCount()).toolCallCount(source.getToolCallCount())
                .startedAt(source.getStartedAt()).finishedAt(source.getFinishedAt()).createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt()).deletedAt(source.getDeletedAt()).build();
    }
}
