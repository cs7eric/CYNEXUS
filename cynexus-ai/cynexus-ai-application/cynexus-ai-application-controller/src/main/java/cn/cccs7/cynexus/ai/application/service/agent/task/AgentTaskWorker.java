package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.service.agent.task.AgentTaskLifecycleDomainService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskWorker {
    private final AgentTaskLifecycleDomainService lifecycleService;
    private final AutonomousAgentAppService agentService;
    private final String workerId = "agent-worker-" + UUID.randomUUID();
    private final AgentTaskLifecycleNotificationService lifecycleNotificationService;

    public AgentTaskWorker(AgentTaskLifecycleDomainService lifecycleService, AutonomousAgentAppService agentService,
                           AgentTaskLifecycleNotificationService lifecycleNotificationService) {
        this.lifecycleService = lifecycleService;
        this.agentService = agentService;
        this.lifecycleNotificationService = lifecycleNotificationService;
    }

    @Scheduled(initialDelayString = "${cynexus.ai.agent.tasks.initial-delay-ms:15000}", fixedDelayString = "${cynexus.ai.agent.tasks.delay-ms:5000}")
    public void processDueTasks() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (AgentTaskBO task : lifecycleService.claimDue(workerId, now, 5, 900)) process(task);
    }

    private void process(AgentTaskBO task) {
        try {
            String runNo = lifecycleService.linkRun(task).orElse(null);
            if (runNo == null) return;
            task.setRunNo(runNo);
            var result = agentService.execute(AgentRunRequest.builder().runNo(runNo).conversationNo(task.getConversationNo())
                    .userInput(task.getUserInput()).preferredModelCode(task.getPreferredModelCode()).idempotencyKey("task:" + task.getTaskNo() + ":" + task.getAttemptCount()).build(),
                    AgentIdentity.builder().tenantKey(task.getTenantKey()).principalKey(task.getPrincipalKey()).build(), ignored -> { });
            task.setRunNo(result.getRunNo());
            if ("SUCCEEDED".equals(result.getStatus()) || "NEEDS_CLARIFICATION".equals(result.getStatus())) {
                lifecycleNotificationService.recordCompleted(task, result.getStatus());
            } else {
                failOrRetry(task, result.getErrorMessage() == null ? "Agent task run did not succeed" : result.getErrorMessage());
            }
        } catch (RuntimeException exception) {
            failOrRetry(task, exception.getMessage() == null ? "Agent task execution failed" : exception.getMessage());
        }
    }

    private void failOrRetry(AgentTaskBO task, String error) {
        var decision = lifecycleService.failureDecision(task, error, OffsetDateTime.now(ZoneOffset.UTC));
        if (decision.type() == AgentTaskLifecycleDomainService.FailureDecision.Type.RETRY) {
            lifecycleNotificationService.recordRetryScheduled(task, decision.nextAttemptAt(), decision.nextAttempt(), decision.error());
        } else {
            lifecycleNotificationService.recordDeadLetter(task, error);
        }
    }
}
