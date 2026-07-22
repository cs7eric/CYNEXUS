package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunStepPO;
import java.util.List;
import java.time.OffsetDateTime;

public interface AgentRunBasicService {
    void create(AgentRunPO run);
    void update(AgentRunPO run);
    void createStep(AgentRunStepPO step);
    AgentRunPO find(String ownerKey, String runNo);
    AgentRunPO findByIdempotencyKey(String ownerKey, String idempotencyKey);
    AgentRunPO requestCancellation(String ownerKey, String runNo, String requestedBy);
    boolean isCancellationRequested(String ownerKey, String runNo);
    List<AgentRunStepPO> findSteps(String ownerKey, String runNo);
    List<AgentRunPO> findStaleRunning(OffsetDateTime cutoff);
}
