package cn.cccs7.cynexus.ai.domain.port.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;

/**
 * Port for durable task lifecycle notifications.
 *
 * <p>The implementation must persist an outbox record. Persisted does not mean
 * delivered: an external notification channel is deliberately outside this
 * contract and must not be reported as successful here.</p>
 */
public interface AgentTaskNotificationPort {
    /** Records the terminal task outcome in the durable outbox. */
    void notifyCompleted(AgentTaskBO task, String resultStatus);

    /** Records the next retry schedule in the durable outbox. */
    void notifyRetryScheduled(AgentTaskBO task, int nextAttempt, String error);

    /** Records a terminal dead-letter transition in the durable outbox. */
    void notifyDeadLetter(AgentTaskBO task, String error);
}
