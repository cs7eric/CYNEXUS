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
public class MarketOverviewBO {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal totalAmount;
    private BigDecimal totalVolume;
    private Integer risingCount;
    private Integer fallingCount;
    private Integer flatCount;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer stockCount;
    private BigDecimal mainNetInflow;
    private BigDecimal superLargeNetInflow;
    private BigDecimal largeNetInflow;
    private BigDecimal mediumNetInflow;
    private BigDecimal smallNetInflow;
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetRatio;
    private BigDecimal largeNetRatio;
    private BigDecimal mediumNetRatio;
    private BigDecimal smallNetRatio;
    private String source;
    private Boolean manualOverride;
    private OffsetDateTime manualUpdatedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
