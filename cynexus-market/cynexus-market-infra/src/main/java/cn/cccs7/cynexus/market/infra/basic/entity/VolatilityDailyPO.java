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
@TableName(value = "msi_volatility_daily", autoResultMap = true)
public class VolatilityDailyPO extends BasePO {

    private LocalDate tradeDate;
    private String underlyingCode;
    private String underlyingName;
    private BigDecimal close;
    private BigDecimal dailyReturnPct;
    @TableField("realized_vol_20d")
    private BigDecimal realizedVol20d;
    private Integer volWindowDays;
    private Integer volLookbackDays;
    @TableField("percentile_1y")
    private BigDecimal percentile1y;
    private BigDecimal sentimentScore;
    private Integer sampleCount;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
