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
public class MarketIndicatorIndustrySnapshotBO {
    private Long id;
    private LocalDate tradeDate;
    private OffsetDateTime capturedAt;
    private String industryCode;
    private String industryName;
    private Integer stockCount;
    private Integer advancingCount;
    private Integer decliningCount;
    private Integer flatCount;
    private Integer limitUpCount;
    private Integer limitUpTouchCount;
    private Integer limitDownCount;
    private Integer limitDownTouchCount;
    private Integer rapidRiseCount;
    private Integer rapidFallCount;
    private OffsetDateTime firstLimitUpAt;
    private String firstLimitUpCode;
    private String firstLimitUpName;
    private BigDecimal firstLimitUpChangePct;
    private OffsetDateTime firstEventAt;
    private String firstEventCode;
    private String firstEventName;
    private String firstEventType;
    private BigDecimal firstEventSignalValue;
    private String source;
}
