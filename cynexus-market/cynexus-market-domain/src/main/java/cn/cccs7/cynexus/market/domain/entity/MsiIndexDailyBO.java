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
public class MsiIndexDailyBO {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal volScore;
    private BigDecimal turnoverScore;
    private BigDecimal priceStrengthScore;
    private BigDecimal riskAppetiteScore;
    private BigDecimal breadthScore;
    private BigDecimal limitEmotionScore;
    private BigDecimal profitEffectScore;
    private BigDecimal sectorBreadthScore;
    private BigDecimal styleRiskScore;
    private BigDecimal compositeScore;
    private Integer componentCount;
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
