package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs safe task lifecycle events until the product notification channel is configured. */
@Component
public class NoopAgentTaskNotificationAdapter implements AgentTaskNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(NoopAgentTaskNotificationAdapter.class);

    @Override
    public void notifyCompleted(AgentTaskBO task, String resultStatus) {
        log.info("Agent task completed taskNo={} status={}", task.getTaskNo(), resultStatus);
    }

    @Override
    public void notifyRetryScheduled(AgentTaskBO task, int nextAttempt, String error) {
        log.warn("Agent task retry scheduled taskNo={} attempt={} reason={}", task.getTaskNo(), nextAttempt, safe(error));
    }

    @Override
    public void notifyDeadLetter(AgentTaskBO task, String error) {
        log.error("Agent task moved to dead letter taskNo={} reason={}", task.getTaskNo(), safe(error));
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.length() <= 300 ? value : value.substring(0, 300);
    }
}
