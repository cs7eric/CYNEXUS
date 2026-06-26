package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_job_statuses")
public class SchedulerJobStatusPO extends BasePO {

    private Long jobId;
    @TableField("is_enabled")
    private Boolean enabled;
    @TableField("is_running")
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
    private String extra;
    private String remark;
}
