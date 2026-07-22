package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentMessagePO;
import java.util.List;

public interface AgentMessageBasicService {
    void create(AgentMessagePO message);
    List<AgentMessagePO> find(String conversationNo);
}
