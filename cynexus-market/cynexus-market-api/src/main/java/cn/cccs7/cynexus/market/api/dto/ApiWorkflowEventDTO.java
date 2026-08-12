package cn.cccs7.cynexus.market.api.dto;

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
    String status;
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
