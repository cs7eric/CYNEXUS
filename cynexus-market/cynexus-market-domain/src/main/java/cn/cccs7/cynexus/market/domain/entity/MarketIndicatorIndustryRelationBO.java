package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorIndustryRelationBO {
    private String industryCode;
    private String industryName;
    private String categoryLabel;
    private Integer overlapCount;
    private Integer sourceStockCount;
    private Integer targetStockCount;
    private Integer advancingCount;
    private Integer decliningCount;
    private Integer flatCount;
    private BigDecimal averageChangePct;
    private BigDecimal averageRelationLevel;
    private BigDecimal relationScore;
    private String relationState;
}
