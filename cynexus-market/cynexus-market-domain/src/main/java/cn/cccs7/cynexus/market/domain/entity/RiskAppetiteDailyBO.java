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
public class RiskAppetiteDailyBO {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal hs300Close;
    private BigDecimal hs300BaseClose;
    private LocalDate hs300BaseDate;
    private BigDecimal hs30020dReturn;
    private BigDecimal treasury511010Close;
    private BigDecimal treasury511010BaseClose;
    private BigDecimal treasury51101020dReturn;
    private BigDecimal treasury511090Close;
    private BigDecimal treasury511090BaseClose;
    private BigDecimal treasury51109020dReturn;
    private BigDecimal treasuryWeighted20dReturn;
    private BigDecimal spread511010;
    private BigDecimal spread511090;
    private BigDecimal spreadWeighted;
    private BigDecimal score;
    private Integer windowDays;
    private String source;
    private Integer elapsedMs;
    private OffsetDateTime ingestedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
