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
public class AiVisionTaskDTO {
    private String id;
    private boolean accepted;
    private String scenarioCode;
    private String status;
    private Integer progress;
    private String executionNo;
    private String providerCode;
    private String modelCode;
    private String content;
    private String errorMessage;
    private String error;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
