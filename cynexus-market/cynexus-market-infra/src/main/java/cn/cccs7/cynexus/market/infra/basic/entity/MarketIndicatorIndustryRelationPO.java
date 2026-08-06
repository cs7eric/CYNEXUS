package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MarketIndicatorIndustryRelationPO {
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
}
