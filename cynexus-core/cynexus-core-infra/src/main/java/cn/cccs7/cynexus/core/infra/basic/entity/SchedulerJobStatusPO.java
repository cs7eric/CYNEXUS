package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class SchedulerJobStatusPO extends BasePO {

    private Long jobId;
    private Boolean isEnabled;
    private Boolean isRunning;
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
    private String extra;
    private String remark;
}
