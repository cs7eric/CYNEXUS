package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionProcessBO {

    private Long id;
    private String ownerKey;
    private String idempotencyKey;
    private String sourceUrl;
    private String downloadUrl;
    private String title;
    private String platform;
    private String noteType;
    private String workerTaskId;
    private Long historyId;
    private String status;
    private String phase;
    private Integer progress;
    private String fileName;
    private String transcript;
    private String polished;
    private String summary;
    private String metadataJson;
    private String errorMessage;
    private Integer attemptCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
