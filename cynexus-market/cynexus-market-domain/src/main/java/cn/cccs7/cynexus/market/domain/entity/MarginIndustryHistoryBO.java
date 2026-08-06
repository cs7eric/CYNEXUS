package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginIndustryHistoryBO {

    private LocalDate tradeDate;
    private LocalDate constituentSnapshotDate;
    private String industryCode;
    private Integer constituentCount;
    private Integer matchedStockCount;
    private BigDecimal financingBalance;
    private BigDecimal financingBuyAmount;
    private BigDecimal financingRepayAmount;
    private BigDecimal securitiesLendingBalance;
    private BigDecimal securitiesLendingBalanceAmount;
    private BigDecimal securitiesLendingSellVolume;
    private BigDecimal securitiesLendingRepayVolume;
    private BigDecimal marginBalance;
    private BigDecimal turnoverAmount;
}
