package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentTaskRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentTaskBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AgentTaskPOConverter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AgentTaskRepositoryImpl implements AgentTaskRepository {
    private final AgentTaskBasicService service;
    public AgentTaskRepositoryImpl(AgentTaskBasicService service) { this.service = service; }
    @Override public AgentTaskBO save(AgentTaskBO task) { return AgentTaskPOConverter.INSTANCE.toTarget(service.save(AgentTaskPOConverter.INSTANCE.toSource(task))); }
    @Override public List<AgentTaskBO> list(String ownerKey, int limit) { return service.list(ownerKey, limit).stream().map(AgentTaskPOConverter.INSTANCE::toTarget).toList(); }
    @Override public Optional<AgentTaskBO> find(String ownerKey, String taskNo) { return Optional.ofNullable(service.find(ownerKey, taskNo)).map(AgentTaskPOConverter.INSTANCE::toTarget); }
    @Override public boolean pause(String ownerKey, String taskNo) { return service.pause(ownerKey, taskNo); }
    @Override public boolean resume(String ownerKey, String taskNo) { return service.resume(ownerKey, taskNo); }
    @Override public boolean cancel(String ownerKey, String taskNo) { return service.cancel(ownerKey, taskNo); }
    @Override public boolean linkRun(AgentTaskBO task, String runNo) { return service.linkRun(AgentTaskPOConverter.INSTANCE.toSource(task), runNo); }
    @Override public List<AgentTaskBO> claimDue(String workerId, OffsetDateTime now, int limit, int leaseSeconds) { return service.claimDue(workerId, now, limit, leaseSeconds).stream().map(AgentTaskPOConverter.INSTANCE::toTarget).toList(); }
    @Override public void complete(AgentTaskBO task) { service.complete(AgentTaskPOConverter.INSTANCE.toSource(task)); }
    @Override public void retry(AgentTaskBO task, OffsetDateTime nextAttemptAt, String error) { service.retry(AgentTaskPOConverter.INSTANCE.toSource(task), nextAttemptAt, error); }
    @Override public void deadLetter(AgentTaskBO task, String error) { service.deadLetter(AgentTaskPOConverter.INSTANCE.toSource(task), error); }
}
