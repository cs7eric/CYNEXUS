package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentTaskRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskWorker {
    private final AgentTaskRepository repository;
    private final AutonomousAgentAppService agentService;
    private final String workerId = "agent-worker-" + UUID.randomUUID();
    private final AgentTaskNotificationPort notificationPort;

    public AgentTaskWorker(AgentTaskRepository repository, AutonomousAgentAppService agentService,
                           AgentTaskNotificationPort notificationPort) {
        this.repository = repository;
        this.agentService = agentService;
        this.notificationPort = notificationPort;
    }

    @Scheduled(initialDelayString = "${cynexus.ai.agent.tasks.initial-delay-ms:15000}", fixedDelayString = "${cynexus.ai.agent.tasks.delay-ms:5000}")
    public void processDueTasks() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (AgentTaskBO task : repository.claimDue(workerId, now, 5, 900)) process(task);
    }

    private void process(AgentTaskBO task) {
        try {
            String runNo = "TASK-RUN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            task.setRunNo(runNo);
            if (!repository.linkRun(task, runNo)) return;
            var result = agentService.execute(AgentRunRequest.builder().runNo(runNo).conversationNo(task.getConversationNo())
                    .userInput(task.getUserInput()).preferredModelCode(task.getPreferredModelCode()).idempotencyKey("task:" + task.getTaskNo() + ":" + task.getAttemptCount()).build(),
                    AgentIdentity.builder().tenantKey(task.getTenantKey()).principalKey(task.getPrincipalKey()).build(), ignored -> { });
            task.setRunNo(result.getRunNo());
            if ("SUCCEEDED".equals(result.getStatus()) || "NEEDS_CLARIFICATION".equals(result.getStatus())) {
                repository.complete(task);
                notificationPort.notifyCompleted(task, result.getStatus());
            } else {
                failOrRetry(task, result.getErrorMessage() == null ? "Agent task run did not succeed" : result.getErrorMessage());
            }
        } catch (RuntimeException exception) {
            failOrRetry(task, exception.getMessage() == null ? "Agent task execution failed" : exception.getMessage());
        }
    }

    private void failOrRetry(AgentTaskBO task, String error) {
        int attempt = task.getAttemptCount() == null ? 1 : task.getAttemptCount();
        if (attempt < (task.getMaxAttempts() == null ? 3 : task.getMaxAttempts())) {
            long backoffSeconds = Math.min(3_600, 30L * (1L << Math.min(6, Math.max(0, attempt - 1))));
            repository.retry(task, OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(backoffSeconds), error);
            notificationPort.notifyRetryScheduled(task, attempt + 1, error);
        } else {
            repository.deadLetter(task, error);
            notificationPort.notifyDeadLetter(task, error);
        }
    }
}
