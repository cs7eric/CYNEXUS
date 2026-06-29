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
@TableName(value = "msi_limit_emotion_daily", autoResultMap = true)
public class MarketSentimentLimitEmotionDailyPO extends BasePO {

    private LocalDate tradeDate;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer touchedCount;
    private Integer brokenCount;
    private BigDecimal breakBoardRate;
    private BigDecimal limitUpDownRatio;
    private Integer yesterdayLimitUpCount;
    private BigDecimal yesterdayLimitUpAvgReturn;
    private BigDecimal upDownScore;
    private BigDecimal breakBoardScore;
    private BigDecimal yesterdayReturnScore;
    private BigDecimal compositeScore;
    private String level;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
