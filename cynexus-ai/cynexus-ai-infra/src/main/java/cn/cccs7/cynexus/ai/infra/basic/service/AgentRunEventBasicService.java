package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunEventPO;
import java.util.List;

public interface AgentRunEventBasicService {
    void create(AgentRunEventPO event);

    List<AgentRunEventPO> find(String ownerKey, String runNo, int afterSequenceNo, int limit);
}
