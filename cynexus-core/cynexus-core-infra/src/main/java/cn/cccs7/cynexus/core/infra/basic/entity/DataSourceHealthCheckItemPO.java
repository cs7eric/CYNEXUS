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
@TableName(value = "data_source_health_check_item", autoResultMap = true)
public class DataSourceHealthCheckItemPO extends BasePO {

    private String runId;
    private String sourceCode;
    private String sourceName;
    private String sourceType;
    private String category;
    private String provider;
    private String description;
    private String endpoint;
    private String status;
    private Boolean connectivity;
    private String authenticityStatus;
    private Integer latencyMs;
    private Integer sampleCount;
    @TableField("success_rate_24h")
    private BigDecimal successRate24h;
    private OffsetDateTime checkedAt;
    private String errorMessage;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String checks;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String rawPayload;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
