package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_job_run", autoResultMap = true)
public class TaskJobRunPO extends BasePO {

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
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String warnings;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String errors;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String params;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;
}
