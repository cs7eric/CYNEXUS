package cn.cccs7.cynexus.ai.application.service.agent.task;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;

/** Application boundary for task notifications; delivery stays outside task execution. */
public interface AgentTaskNotificationPort {
    void notifyCompleted(AgentTaskBO task, String resultStatus);
    void notifyRetryScheduled(AgentTaskBO task, int nextAttempt, String error);
    void notifyDeadLetter(AgentTaskBO task, String error);
}
