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
public class MaCountDailyBO {

    private Long id;
    private LocalDate tradeDate;
    private Integer totalEligible;
    private Integer aboveMa20;
    private Integer aboveMa60;
    private Integer aboveBoth;
    private BigDecimal pctMa20;
    private BigDecimal pctMa60;
    private BigDecimal pctBoth;
    private String byBoard;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private Integer up5dCount;
    private BigDecimal up5dPct;
    private Integer newLow60dCount;
    private BigDecimal newLow60dPct;
    private Integer newHigh252dCount;
    private BigDecimal newHigh252dPct;
    private Integer advancingCount;
    private BigDecimal advancingPct;
    private BigDecimal breadthRaw;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
