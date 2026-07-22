package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunEventPO;
import java.util.List;

public interface AgentRunEventBasicService {
    void create(AgentRunEventPO event);

    List<AgentRunEventPO> find(String ownerKey, String runNo, int afterSequenceNo, int limit);
}
