package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiWorkflowProcessDTO {
    String id;
    String workflowType;
    String workflowKey;
    String source;
    String sourceRef;
    String title;
    String status;
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
