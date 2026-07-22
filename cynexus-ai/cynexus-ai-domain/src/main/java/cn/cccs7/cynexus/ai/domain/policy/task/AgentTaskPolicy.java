package cn.cccs7.cynexus.ai.domain.agent;

import java.time.OffsetDateTime;

public final class AgentTaskPolicy {
    private AgentTaskPolicy() {
    }

    public static void validate(String userInput, OffsetDateTime scheduleAt, int maxAttempts) {
        if (userInput == null || userInput.isBlank() || userInput.length() > 12_000) throw new IllegalArgumentException("Task input must contain 1-12000 characters");
        if (scheduleAt == null || scheduleAt.isBefore(OffsetDateTime.now().minusMinutes(1))) throw new IllegalArgumentException("Task schedule must be in the future");
        if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("maxAttempts must be between 1 and 5");
    }
}
