package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_job_run_history")
public class SchedulerJobRunHistoryPO extends BasePO {

    private Long jobId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String triggerType;
    private String status;
    private String errorMessage;
    private BigDecimal durationSeconds;
    private String remark;
}
