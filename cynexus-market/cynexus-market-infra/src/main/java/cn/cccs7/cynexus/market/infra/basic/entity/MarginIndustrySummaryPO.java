package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class MarginIndustrySummaryPO {

    private LocalDate tradeDate;
    private LocalDate constituentSnapshotDate;
    private String industryCode;
    private String industryName;
    private String tdxCode;
    private String tdxName;
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
}
