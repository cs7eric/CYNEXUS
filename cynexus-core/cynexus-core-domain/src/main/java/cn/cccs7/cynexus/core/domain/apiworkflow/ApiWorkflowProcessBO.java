package cn.cccs7.cynexus.core.domain.apiworkflow;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiWorkflowProcessBO(
        String id,
        ApiWorkflowType workflowType,
        String workflowKey,
        String source,
        String sourceRef,
        String title,
        ApiWorkflowStatus status,
        String phase,
        Integer progress,
        Integer attemptCount,
        String errorCode,
        String errorMessage,
        String resultRoute,
        String resultRef,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime updatedAt,
        List<ApiWorkflowEventBO> events
) {
    public ApiWorkflowProcessBO {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
