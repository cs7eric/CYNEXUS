package cn.cccs7.cynexus.core.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobStatusDTO {

    private Long id;
    private Long jobId;
    private Boolean enabled;
    private Boolean running;
    private String schedule;
    private Integer timezoneOffsetHours;
    private Integer tickSeconds;
    private OffsetDateTime lastRunAt;
    private Boolean lastRunOk;
    private String lastRunStatus;
    private String lastRunErrorMessage;
    private BigDecimal lastDurationSeconds;
    private Integer lastTargetsProcessed;
    private Integer totalRuns;
    private Integer totalFailures;
    private OffsetDateTime schedulerStartedAt;
    private OffsetDateTime stoppedAt;
    private String progress;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
