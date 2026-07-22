package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentTaskPO;
import java.time.OffsetDateTime;
import java.util.List;

public interface AgentTaskBasicService {
    AgentTaskPO save(AgentTaskPO task);
    List<AgentTaskPO> list(String ownerKey, int limit);
    AgentTaskPO find(String ownerKey, String taskNo);
    boolean pause(String ownerKey, String taskNo);
    boolean resume(String ownerKey, String taskNo);
    boolean cancel(String ownerKey, String taskNo);
    List<AgentTaskPO> claimDue(String workerId, OffsetDateTime now, int limit, int leaseSeconds);
    void complete(AgentTaskPO task);
    void retry(AgentTaskPO task, OffsetDateTime nextAttemptAt, String error);
    void deadLetter(AgentTaskPO task, String error);
}
