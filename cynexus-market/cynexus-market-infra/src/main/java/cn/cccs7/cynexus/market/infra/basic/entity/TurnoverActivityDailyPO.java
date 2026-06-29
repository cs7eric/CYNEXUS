package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "msi_turnover_activity_daily", autoResultMap = true)
public class TurnoverActivityDailyPO extends BasePO {

    private LocalDate tradeDate;
    private BigDecimal totalAmount;
    private BigDecimal shAmount;
    private BigDecimal szAmount;
    @TableField("avg_20d_amount")
    private BigDecimal avg20dAmount;
    private BigDecimal ratio;
    private Integer sampleCount;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private BigDecimal score;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
