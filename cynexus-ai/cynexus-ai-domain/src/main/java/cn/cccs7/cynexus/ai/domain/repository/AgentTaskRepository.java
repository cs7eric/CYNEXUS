package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentTaskBO;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository {
    AgentTaskBO save(AgentTaskBO task);
    List<AgentTaskBO> list(String ownerKey, int limit);
    Optional<AgentTaskBO> find(String ownerKey, String taskNo);
    boolean pause(String ownerKey, String taskNo);
    boolean resume(String ownerKey, String taskNo);
    boolean cancel(String ownerKey, String taskNo);
    boolean linkRun(AgentTaskBO task, String runNo);
    List<AgentTaskBO> claimDue(String workerId, OffsetDateTime now, int limit, int leaseSeconds);
    void complete(AgentTaskBO task);
    void retry(AgentTaskBO task, OffsetDateTime nextAttemptAt, String error);
    void deadLetter(AgentTaskBO task, String error);
}
