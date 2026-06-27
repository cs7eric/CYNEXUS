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
@TableName(value = "msi_risk_appetite_daily", autoResultMap = true)
public class RiskAppetiteDailyPO extends BasePO {

    private LocalDate tradeDate;
    private BigDecimal hs300Close;
    private BigDecimal hs300BaseClose;
    private LocalDate hs300BaseDate;
    @TableField("hs300_20d_return")
    private BigDecimal hs30020dReturn;
    @TableField("treasury_511010_close")
    private BigDecimal treasury511010Close;
    @TableField("treasury_511010_base_close")
    private BigDecimal treasury511010BaseClose;
    @TableField("treasury_511010_20d_return")
    private BigDecimal treasury51101020dReturn;
    @TableField("treasury_511090_close")
    private BigDecimal treasury511090Close;
    @TableField("treasury_511090_base_close")
    private BigDecimal treasury511090BaseClose;
    @TableField("treasury_511090_20d_return")
    private BigDecimal treasury51109020dReturn;
    @TableField("treasury_weighted_20d_return")
    private BigDecimal treasuryWeighted20dReturn;
    @TableField("spread_511010")
    private BigDecimal spread511010;
    @TableField("spread_511090")
    private BigDecimal spread511090;
    private BigDecimal spreadWeighted;
    private Integer windowDays;
    private String source;
    private Integer elapsedMs;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
