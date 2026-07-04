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
public class MarginAccountStatsBO {

    private Long id;
    private LocalDate tradeDate;
    private String source;
    private BigDecimal financingBalance;
    private BigDecimal securitiesLendingBalance;
    private BigDecimal financingBuyAmount;
    private BigDecimal securitiesLendingSellAmount;
    private Integer brokerCount;
    private Integer businessDepartmentCount;
    private Integer personalInvestorCount;
    private Integer institutionInvestorCount;
    private Integer activeInvestorCount;
    private Integer liabilityInvestorCount;
    private BigDecimal totalGuarantee;
    private BigDecimal averageGuaranteeRatio;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
