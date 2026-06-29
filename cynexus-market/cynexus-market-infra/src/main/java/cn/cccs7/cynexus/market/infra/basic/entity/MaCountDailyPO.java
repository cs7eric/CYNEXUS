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
@TableName(value = "msi_ma_count_daily", autoResultMap = true)
public class MaCountDailyPO extends BasePO {

    private LocalDate tradeDate;
    private Integer totalEligible;
    private Integer aboveMa20;
    private Integer aboveMa60;
    private Integer aboveBoth;
    private BigDecimal pctMa20;
    private BigDecimal pctMa60;
    private BigDecimal pctBoth;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String byBoard;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    @TableField("up_5d_count")
    private Integer up5dCount;
    @TableField("up_5d_pct")
    private BigDecimal up5dPct;
    @TableField("new_low_60d_count")
    private Integer newLow60dCount;
    @TableField("new_low_60d_pct")
    private BigDecimal newLow60dPct;
    @TableField("new_high_252d_count")
    private Integer newHigh252dCount;
    @TableField("new_high_252d_pct")
    private BigDecimal newHigh252dPct;
    private Integer advancingCount;
    private BigDecimal advancingPct;
    private BigDecimal breadthRaw;
    private BigDecimal priceStrengthScore;
    private BigDecimal breadthScore;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
