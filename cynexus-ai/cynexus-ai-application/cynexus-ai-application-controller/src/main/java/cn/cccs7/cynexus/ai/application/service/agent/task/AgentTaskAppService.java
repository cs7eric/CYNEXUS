package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.api.dto.AgentTaskDTO;
import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.service.agent.task.AgentTaskLifecycleDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskAppService {
    private final AgentTaskLifecycleDomainService lifecycleService;

    public AgentTaskAppService(AgentTaskLifecycleDomainService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    public AgentTaskDTO schedule(AgentIdentity identity, AgentTaskRequest request) {
        AgentIdentity safe = identity == null ? AgentIdentity.singleUser() : identity;
        int maxAttempts = request == null || request.getMaxAttempts() == null ? 3 : request.getMaxAttempts();
        AgentTaskBO task = lifecycleService.schedule(new cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentTaskScheduleCommand(
                safe,
                request == null ? null : request.getConversationNo(),
                request == null ? null : request.getUserInput(),
                request == null ? null : request.getPreferredModelCode(),
                request == null ? null : request.getScheduleAt(),
                maxAttempts));
        return toDTO(task);
    }

    public List<AgentTaskDTO> list(AgentIdentity identity, int limit) {
        return lifecycleService.list(owner(identity), limit).stream().map(this::toDTO).toList();
    }

    public AgentTaskDTO find(AgentIdentity identity, String taskNo) {
        return lifecycleService.find(owner(identity), taskNo).map(this::toDTO).orElse(null);
    }

    public boolean pause(AgentIdentity identity, String taskNo) { return lifecycleService.pause(owner(identity), taskNo); }
    public boolean resume(AgentIdentity identity, String taskNo) { return lifecycleService.resume(owner(identity), taskNo); }
    public boolean cancel(AgentIdentity identity, String taskNo) { return lifecycleService.cancel(identity, owner(identity), taskNo); }

    private String owner(AgentIdentity identity) { return identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey(); }

    private AgentTaskDTO toDTO(AgentTaskBO task) { return AgentTaskDTO.builder().taskNo(task.getTaskNo()).conversationNo(task.getConversationNo()).userInput(task.getUserInput()).preferredModelCode(task.getPreferredModelCode()).scheduleAt(task.getScheduleAt()).status(task.getStatus()).attemptCount(task.getAttemptCount()).maxAttempts(task.getMaxAttempts()).runNo(task.getRunNo()).lastError(task.getLastError()).nextAttemptAt(task.getNextAttemptAt()).completedAt(task.getCompletedAt()).build(); }
}
