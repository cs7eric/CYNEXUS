package cn.cccs7.cynexus.core.application.dto;

import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowEventDTO {
    String eventType;
    String phase;
    ApiWorkflowStatus status;
    Integer progress;
    String message;
    String errorCode;
    String errorMessage;
    OffsetDateTime occurredAt;
}
