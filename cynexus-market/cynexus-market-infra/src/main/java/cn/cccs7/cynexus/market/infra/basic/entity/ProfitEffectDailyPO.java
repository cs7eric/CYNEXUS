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
@TableName(value = "msi_profit_effect_daily", autoResultMap = true)
public class ProfitEffectDailyPO extends BasePO {

    private LocalDate tradeDate;
    @TableField("up_5d_pct")
    private BigDecimal up5dPct;
    @TableField("new_low_60d_pct")
    private BigDecimal newLow60dPct;
    private BigDecimal score;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
