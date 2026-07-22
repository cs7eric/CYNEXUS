package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AgentTaskDTO;
import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentTaskPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AgentTaskRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskAppService {
    private final AgentTaskRepository repository;
    private final AutonomousAgentAppService agentService;
    public AgentTaskAppService(AgentTaskRepository repository, AutonomousAgentAppService agentService) {
        this.repository = repository;
        this.agentService = agentService;
    }

    public AgentTaskDTO schedule(AgentIdentity identity, AgentTaskRequest request) {
        AgentIdentity safe = identity == null ? AgentIdentity.singleUser() : identity;
        int maxAttempts = request == null || request.getMaxAttempts() == null ? 3 : request.getMaxAttempts();
        AgentTaskPolicy.validate(request == null ? null : request.getUserInput(), request == null ? null : request.getScheduleAt(), maxAttempts);
        OffsetDateTime scheduleAt = request.getScheduleAt();
        AgentTaskBO task = repository.save(AgentTaskBO.builder().taskNo("ATK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .ownerKey(safe.storageOwnerKey()).tenantKey(safe.getTenantKey()).principalKey(safe.getPrincipalKey())
                .conversationNo(request.getConversationNo()).userInput(request.getUserInput().trim()).preferredModelCode(request.getPreferredModelCode())
                .scheduleAt(scheduleAt).nextAttemptAt(scheduleAt).status("QUEUED").attemptCount(0).maxAttempts(maxAttempts).build());
        return toDTO(task);
    }

    public List<AgentTaskDTO> list(AgentIdentity identity, int limit) {
        String owner = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        return repository.list(owner, limit).stream().map(this::toDTO).toList();
    }

    public AgentTaskDTO find(AgentIdentity identity, String taskNo) {
        String owner = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        return repository.find(owner, taskNo).map(this::toDTO).orElse(null);
    }

    public boolean pause(AgentIdentity identity, String taskNo) { return repository.pause(owner(identity), taskNo); }
    public boolean resume(AgentIdentity identity, String taskNo) { return repository.resume(owner(identity), taskNo); }
    public boolean cancel(AgentIdentity identity, String taskNo) {
        String ownerKey = owner(identity);
        AgentTaskBO task = repository.find(ownerKey, taskNo).orElse(null);
        if (task == null) return false;
        if ("RUNNING".equals(task.getStatus()) && task.getRunNo() != null && !task.getRunNo().isBlank()) {
            agentService.cancel(identity == null ? AgentIdentity.singleUser() : identity, task.getRunNo());
        }
        return repository.cancel(ownerKey, taskNo);
    }
    private String owner(AgentIdentity identity) { return identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey(); }
    private AgentTaskDTO toDTO(AgentTaskBO task) { return AgentTaskDTO.builder().taskNo(task.getTaskNo()).conversationNo(task.getConversationNo()).userInput(task.getUserInput()).preferredModelCode(task.getPreferredModelCode()).scheduleAt(task.getScheduleAt()).status(task.getStatus()).attemptCount(task.getAttemptCount()).maxAttempts(task.getMaxAttempts()).runNo(task.getRunNo()).lastError(task.getLastError()).nextAttemptAt(task.getNextAttemptAt()).completedAt(task.getCompletedAt()).build(); }
}
