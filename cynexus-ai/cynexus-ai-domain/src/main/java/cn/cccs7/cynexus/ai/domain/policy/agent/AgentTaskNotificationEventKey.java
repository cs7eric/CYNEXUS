package cn.cccs7.cynexus.ai.domain.policy.agent;

import java.util.Objects;

/** Stable event-key policy used to make task lifecycle notifications idempotent. */
public final class AgentTaskNotificationEventKey {
    public static final String COMPLETED = "COMPLETED";
    public static final String RETRY_SCHEDULED = "RETRY_SCHEDULED";
    public static final String DEAD_LETTER = "DEAD_LETTER";
    private static final int MAX_EVENT_KEY_LENGTH = 256;

    private AgentTaskNotificationEventKey() {
    }

    public static String completed(String taskNo) {
        return base(taskNo) + ":COMPLETED";
    }

    public static String retryScheduled(String taskNo, int nextAttempt) {
        if (nextAttempt < 1) {
            throw new IllegalArgumentException("nextAttempt must be positive");
        }
        return base(taskNo) + ":RETRY:" + nextAttempt;
    }

    public static String deadLetter(String taskNo) {
        return base(taskNo) + ":DEAD_LETTER";
    }

    /**
     * Validates an event key at the domain boundary before it reaches the
     * persistence adapter. The database unique constraint remains the final
     * idempotency guard for concurrent writers.
     */
    public static String requireEventKey(String eventKey) {
        String normalized = Objects.requireNonNull(eventKey, "eventKey").trim();
        if (normalized.isEmpty() || normalized.length() > MAX_EVENT_KEY_LENGTH) {
            throw new IllegalArgumentException("eventKey must contain 1-256 characters");
        }
        return normalized;
    }

    public static String requireEventType(String eventType) {
        String normalized = Objects.requireNonNull(eventType, "eventType").trim();
        if (!COMPLETED.equals(normalized) && !RETRY_SCHEDULED.equals(normalized)
                && !DEAD_LETTER.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported task notification event type");
        }
        return normalized;
    }

    private static String base(String taskNo) {
        String normalized = Objects.requireNonNull(taskNo, "taskNo").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("taskNo must not be blank");
        }
        return normalized;
    }
}
