package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.time.OffsetDateTime;

public record ApiWorkflowEventBO(
        Integer attemptNo,
        String eventType,
        String fromStatus,
        String toStatus,
        String phase,
        ApiWorkflowStatus status,
        Integer progress,
        String message,
        String providerCode,
        String modelCode,
        String errorCode,
        String errorMessage,
        String details,
        Long durationMs,
        OffsetDateTime occurredAt
) {
}
