package cn.cccs7.cynexus.market.api.dto;

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
public class VolatilityDailyDTO {

    private Long id;
    private LocalDate tradeDate;
    private String underlyingCode;
    private String underlyingName;
    private BigDecimal close;
    private BigDecimal dailyReturnPct;
    private BigDecimal realizedVol20d;
    private Integer volWindowDays;
    private Integer volLookbackDays;
    private BigDecimal percentile1y;
    private BigDecimal sentimentScore;
    private Integer sampleCount;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
