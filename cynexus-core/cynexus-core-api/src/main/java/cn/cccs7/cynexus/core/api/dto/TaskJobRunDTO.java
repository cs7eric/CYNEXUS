package cn.cccs7.cynexus.core.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskJobRunDTO {

    private String runId;
    private String jobKey;
    private String triggerRequestId;
    private String triggeredBy;
    private Boolean dryRun;
    private String status;
    private Boolean success;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long rowsRead;
    private Long rowsWritten;
    private String warnings;
    private String errors;
    private String params;
    private String metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
