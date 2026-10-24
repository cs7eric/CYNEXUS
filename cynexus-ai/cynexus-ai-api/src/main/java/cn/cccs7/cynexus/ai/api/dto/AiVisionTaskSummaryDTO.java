package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVisionTaskSummaryDTO {
    private String id;
    private String scenarioCode;
    private String idempotencyKey;
    private String status;
    private Integer progress;
    private Integer attemptCount;
    private Integer maxAttempts;
    private boolean retryable;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
