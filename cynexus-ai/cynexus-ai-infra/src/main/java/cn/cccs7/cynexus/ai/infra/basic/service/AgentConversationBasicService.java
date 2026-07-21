package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentConversationPO;
import java.util.Optional;
import java.util.List;

public interface AgentConversationBasicService {
    void create(AgentConversationPO conversation);
    void update(AgentConversationPO conversation);
    Optional<AgentConversationPO> find(String ownerKey, String conversationNo);
    List<AgentConversationPO> list(String ownerKey, int limit, String query);
}
