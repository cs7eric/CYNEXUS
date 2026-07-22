package cn.cccs7.cynexus.ai.infra.notification.agent.task;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentTaskNotificationEventKey;
import cn.cccs7.cynexus.ai.domain.port.agent.AgentTaskNotificationPort;
import cn.cccs7.cynexus.ai.infra.repository.task.AgentTaskNotificationRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Persists lifecycle events to the Agent outbox. This adapter intentionally
 * has no external channel client; a successful call means only that the
 * outbox insert completed (or was an idempotent duplicate).
 */
@Primary
@Component
public class PersistentAgentTaskNotificationAdapter implements AgentTaskNotificationPort {
    private final AgentTaskNotificationRepository repository;

    public PersistentAgentTaskNotificationAdapter(AgentTaskNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void notifyCompleted(AgentTaskBO task, String resultStatus) {
        repository.append(AgentTaskNotificationEventKey.completed(task.getTaskNo()),
                AgentTaskNotificationEventKey.COMPLETED, task,
                payload("resultStatus", safe(resultStatus)));
    }

    @Override
    public void notifyRetryScheduled(AgentTaskBO task, int nextAttempt, String error) {
        repository.append(AgentTaskNotificationEventKey.retryScheduled(task.getTaskNo(), nextAttempt),
                AgentTaskNotificationEventKey.RETRY_SCHEDULED, task,
                Map.of("nextAttempt", nextAttempt, "error", safe(error)));
    }

    @Override
    public void notifyDeadLetter(AgentTaskBO task, String error) {
        repository.append(AgentTaskNotificationEventKey.deadLetter(task.getTaskNo()),
                AgentTaskNotificationEventKey.DEAD_LETTER, task,
                Map.of("error", safe(error)));
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private Map<String, Object> payload(String key, String value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(Objects.requireNonNull(key, "key"), value);
        return payload;
    }
}
