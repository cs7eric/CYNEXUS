package cn.cccs7.cynexus.common.messaging.outbox;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;

import java.time.Instant;

public record OutboxRecord(
        String id,
        MessageDestination destination,
        MessageEnvelope envelope,
        Instant availableAt,
        int attemptCount
) {

    public OutboxRecord {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (destination == null || envelope == null || availableAt == null) {
            throw new IllegalArgumentException("destination, envelope and availableAt are required");
        }
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
    }
}
