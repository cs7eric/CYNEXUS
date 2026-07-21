package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunStepPO;
import java.util.List;

public interface AgentRunBasicService {
    void create(AgentRunPO run);
    void update(AgentRunPO run);
    void createStep(AgentRunStepPO step);
    AgentRunPO find(String ownerKey, String runNo);
    List<AgentRunStepPO> findSteps(String ownerKey, String runNo);
}
