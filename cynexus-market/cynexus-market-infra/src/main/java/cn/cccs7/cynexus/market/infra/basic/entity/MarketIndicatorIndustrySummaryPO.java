package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketIndicatorIndustrySummaryPO {
    private String industryCode;
    private String industryName;
    private String categoryLabel;
    private Integer totalCount;
    private Integer advancingCount;
    private Integer decliningCount;
    private Integer flatCount;
    private BigDecimal averageChangePct;
    private OffsetDateTime capturedAt;
}
