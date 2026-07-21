package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentConversationPO;
import java.util.Optional;

public interface AgentConversationBasicService {
    void create(AgentConversationPO conversation);
    void update(AgentConversationPO conversation);
    Optional<AgentConversationPO> find(String ownerKey, String conversationNo);
}
