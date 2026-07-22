package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentConversationPO;
public final class AgentConversationPOConverter {
    public static final AgentConversationPOConverter INSTANCE = new AgentConversationPOConverter();

    private AgentConversationPOConverter() {
    }

    public AgentConversationPO toSource(AgentConversationBO source) {
        if (source == null) return null;
        AgentConversationPO target = new AgentConversationPO();
        target.setId(source.getId()); target.setConversationNo(source.getConversationNo());
        target.setOwnerKey(source.getOwnerKey()); target.setTitle(source.getTitle()); target.setStatus(source.getStatus());
        target.setLastRunNo(source.getLastRunNo()); target.setLastMessage(source.getLastMessage());
        target.setMetadata(source.getMetadataJson()); target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt()); target.setDeletedAt(source.getDeletedAt());
        return target;
    }

    public AgentConversationBO toTarget(AgentConversationPO source) {
        if (source == null) return null;
        return AgentConversationBO.builder().id(source.getId()).conversationNo(source.getConversationNo())
                .ownerKey(source.getOwnerKey()).title(source.getTitle()).status(source.getStatus())
                .lastRunNo(source.getLastRunNo()).lastMessage(source.getLastMessage()).metadataJson(source.getMetadata())
                .createdAt(source.getCreatedAt()).updatedAt(source.getUpdatedAt()).deletedAt(source.getDeletedAt()).build();
    }
}
