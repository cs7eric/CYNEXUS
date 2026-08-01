package cn.cccs7.cynexus.common.messaging.api;

import java.time.Instant;
import java.util.Map;

public record MessageEnvelope(
        String messageId,
        MessageType messageType,
        String name,
        int schemaVersion,
        String producer,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String traceId,
        String correlationId,
        String causationId,
        String payloadJson,
        Map<String, String> headers
) {

    public MessageEnvelope {
        requireText(messageId, "messageId");
        if (messageType == null) {
            throw new IllegalArgumentException("messageType must not be null");
        }
        requireText(name, "name");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        requireText(producer, "producer");
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        requireText(payloadJson, "payloadJson");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
