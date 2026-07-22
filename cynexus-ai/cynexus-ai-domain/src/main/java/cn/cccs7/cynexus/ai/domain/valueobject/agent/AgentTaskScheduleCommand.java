package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import java.time.OffsetDateTime;

/** Domain input for scheduling an agent task; it is independent of HTTP DTOs. */
public record AgentTaskScheduleCommand(
        AgentIdentity identity,
        String conversationNo,
        String userInput,
        String preferredModelCode,
        OffsetDateTime scheduleAt,
        int maxAttempts) {
}
