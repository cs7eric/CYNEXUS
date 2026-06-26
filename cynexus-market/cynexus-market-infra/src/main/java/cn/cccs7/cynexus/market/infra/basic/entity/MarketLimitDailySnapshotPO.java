package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "market_limit_daily_snapshots", autoResultMap = true)
public class MarketLimitDailySnapshotPO extends BasePO {

    private LocalDate tradeDate;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer touchedCount;
    private Integer brokenCount;
    private BigDecimal breakBoardRate;
    private Integer maxStreakHeight;
    private BigDecimal promotionOverallRate;
    private String sentimentLevel;
    private String sentimentText;
    private Integer stockCount;
    private String marketStatus;
    private String dataStatus;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
