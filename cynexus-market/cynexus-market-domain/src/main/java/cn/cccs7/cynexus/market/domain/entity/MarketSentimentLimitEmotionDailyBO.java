package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSentimentLimitEmotionDailyBO {

    private Long id;
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
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
