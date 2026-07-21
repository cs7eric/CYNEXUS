package cn.cccs7.cynexus.core.application.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscriptionProcessDTO {

    private String id;
    private Boolean accepted;
    private String sourceUrl;
    private String downloadUrl;
    private String title;
    private String platform;
    private String noteType;
    private String workerTaskId;
    private String historyId;
    private String status;
    private String phase;
    private Integer progress;
    private String fileName;
    private String transcript;
    private String polished;
    private String summary;
    private Map<String, Object> metadata;
    private String errorMessage;
    private Integer attemptCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
