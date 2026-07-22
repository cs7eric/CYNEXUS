package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentMessagePO;

public final class AgentMessagePOConverter {
    public static final AgentMessagePOConverter INSTANCE = new AgentMessagePOConverter();

    private AgentMessagePOConverter() {
    }

    public AgentMessagePO toSource(AgentMessageBO source) {
        if (source == null) return null;
        AgentMessagePO target = new AgentMessagePO();
        target.setId(source.getId());
        target.setConversationNo(source.getConversationNo());
        target.setRunNo(source.getRunNo());
        target.setSequenceNo(source.getSequenceNo());
        target.setRole(source.getRole());
        target.setContent(source.getContent());
        target.setToolCallId(source.getToolCallId());
        target.setToolName(source.getToolName());
        target.setToolCalls(source.getToolCallsJson());
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    public AgentMessageBO toTarget(AgentMessagePO source) {
        if (source == null) return null;
        return AgentMessageBO.builder().id(source.getId()).conversationNo(source.getConversationNo())
                .runNo(source.getRunNo()).sequenceNo(source.getSequenceNo()).role(source.getRole())
                .content(source.getContent()).toolCallId(source.getToolCallId()).toolName(source.getToolName())
                .toolCallsJson(source.getToolCalls()).createdAt(source.getCreatedAt()).build();
    }
}
