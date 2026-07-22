package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentTaskNotificationPort;
import cn.cccs7.cynexus.ai.domain.service.agent.task.AgentTaskLifecycleDomainService;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits a task lifecycle transition and its durable outbox record together.
 * The notification port records PENDING persistence only; no external delivery
 * is reported from this service.
 */
@Service
public class AgentTaskLifecycleNotificationService {
    private final AgentTaskLifecycleDomainService lifecycleService;
    private final AgentTaskNotificationPort notificationPort;

    public AgentTaskLifecycleNotificationService(AgentTaskLifecycleDomainService lifecycleService,
                                                 AgentTaskNotificationPort notificationPort) {
        this.lifecycleService = lifecycleService;
        this.notificationPort = notificationPort;
    }

    @Transactional
    public void recordCompleted(AgentTaskBO task, String resultStatus) {
        lifecycleService.complete(task);
        notificationPort.notifyCompleted(task, resultStatus);
    }

    @Transactional
    public void recordRetryScheduled(AgentTaskBO task, OffsetDateTime nextAttemptAt,
                                     int nextAttempt, String error) {
        lifecycleService.retry(task, nextAttemptAt, nextAttempt, error);
        notificationPort.notifyRetryScheduled(task, nextAttempt, error);
    }

    @Transactional
    public void recordDeadLetter(AgentTaskBO task, String error) {
        lifecycleService.deadLetter(task, error);
        notificationPort.notifyDeadLetter(task, error);
    }
}
