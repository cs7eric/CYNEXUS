package cn.cccs7.cynexus.ai.domain.policy.task;

import java.time.OffsetDateTime;

public final class AgentTaskPolicy {
    private AgentTaskPolicy() {
    }

    public static void validate(String userInput, OffsetDateTime scheduleAt, int maxAttempts) {
        validate(userInput, scheduleAt, maxAttempts, OffsetDateTime.now());
    }

    public static void validate(String userInput, OffsetDateTime scheduleAt, int maxAttempts, OffsetDateTime now) {
        if (userInput == null || userInput.isBlank() || userInput.length() > 12_000) throw new IllegalArgumentException("Task input must contain 1-12000 characters");
        if (scheduleAt == null || scheduleAt.isBefore(now.minusMinutes(1))) throw new IllegalArgumentException("Task schedule must be in the future");
        if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("maxAttempts must be between 1 and 5");
    }

    public static long retryBackoffSeconds(int attempt) {
        int exponent = Math.min(6, Math.max(0, attempt - 1));
        return Math.min(3_600L, 30L * (1L << exponent));
    }
}
