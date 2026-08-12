package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowEventDTO {
    String eventType;
    String phase;
    String status;
    Integer progress;
    String message;
    String errorCode;
    String errorMessage;
    OffsetDateTime occurredAt;
}
