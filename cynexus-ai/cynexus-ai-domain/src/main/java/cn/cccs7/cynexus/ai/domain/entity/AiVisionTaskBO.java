package cn.cccs7.cynexus.ai.domain.entity;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persisted lifecycle state for an asynchronous vision execution. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVisionTaskBO {
    private Long id;
    private String scenarioCode;
    private String userInput;
    private String preferredModelCode;
    private String idempotencyKey;
    private List<Image> images;
    private String tenantKey;
    private String principalKey;
    private String status;
    private Integer progress;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String claimToken;
    private boolean hasRetry;
    private boolean inputsAvailable;
    private String executionNo;
    private String providerCode;
    private String modelCode;
    private String content;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String fileName;
        private String contentType;
        private String dataUrl;
    }
}
