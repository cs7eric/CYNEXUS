package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "data_source_health_check_run", autoResultMap = true)
public class DataSourceHealthCheckRunPO extends BasePO {

    private String runId;
    private String triggerType;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Integer durationMs;
    private Boolean includeSlow;
    private Integer totalCount;
    private Integer healthyCount;
    private Integer degradedCount;
    private Integer failedCount;
    private Integer skippedCount;
    private BigDecimal successRate;
    private BigDecimal avgLatencyMs;
    private String errorMessage;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String rawPayload;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
