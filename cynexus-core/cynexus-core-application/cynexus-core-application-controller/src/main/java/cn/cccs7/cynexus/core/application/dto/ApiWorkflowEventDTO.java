package cn.cccs7.cynexus.core.application.dto;

import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowEventDTO {
    Integer attemptNo;
    String eventType;
    String fromStatus;
    String toStatus;
    String phase;
    ApiWorkflowStatus status;
    Integer progress;
    String message;
    String providerCode;
    String modelCode;
    String errorCode;
    String errorMessage;
    String details;
    Long durationMs;
    OffsetDateTime occurredAt;
}
