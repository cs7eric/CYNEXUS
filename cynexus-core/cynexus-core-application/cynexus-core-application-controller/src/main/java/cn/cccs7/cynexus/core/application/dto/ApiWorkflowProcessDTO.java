package cn.cccs7.cynexus.core.application.dto;

import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowStatus;
import cn.cccs7.cynexus.core.domain.apiworkflow.ApiWorkflowType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowProcessDTO {
    String id;
    ApiWorkflowType workflowType;
    String workflowKey;
    String source;
    String sourceRef;
    String title;
    ApiWorkflowStatus status;
    String phase;
    Integer progress;
    Integer attemptCount;
    String errorCode;
    String errorMessage;
    String resultRoute;
    String resultRef;
    OffsetDateTime createdAt;
    OffsetDateTime startedAt;
    OffsetDateTime completedAt;
    OffsetDateTime updatedAt;
    List<ApiWorkflowEventDTO> events;
}
