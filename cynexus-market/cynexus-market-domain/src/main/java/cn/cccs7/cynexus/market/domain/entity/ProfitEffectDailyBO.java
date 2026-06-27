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
public class ProfitEffectDailyBO {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal up5dPct;
    private BigDecimal newLow60dPct;
    private BigDecimal score;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
