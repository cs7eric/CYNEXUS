package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.time.OffsetDateTime;

public record ApiWorkflowEventBO(
        String eventType,
        String phase,
        ApiWorkflowStatus status,
        Integer progress,
        String message,
        String errorCode,
        String errorMessage,
        OffsetDateTime occurredAt
) {
}
