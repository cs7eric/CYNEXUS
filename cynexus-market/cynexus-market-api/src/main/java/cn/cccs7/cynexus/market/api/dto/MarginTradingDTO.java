package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class MarginTradingDTO {

    private MarginTradingDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketSummaryDTO {
        private LocalDate tradeDate;
        private String exchange;
        private String source;
        private BigDecimal financingBalance;
        private BigDecimal financingBuyAmount;
        private BigDecimal financingRepayAmount;
        private BigDecimal securitiesLendingBalance;
        private BigDecimal securitiesLendingBalanceAmount;
        private BigDecimal securitiesLendingSellVolume;
        private BigDecimal securitiesLendingRepayVolume;
        private BigDecimal marginBalance;
        private String extra;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockDetailDTO {
        private LocalDate tradeDate;
        private String exchange;
        private String stockCode;
        private String stockName;
        private String source;
        private BigDecimal financingBalance;
        private BigDecimal financingBuyAmount;
        private BigDecimal financingRepayAmount;
        private BigDecimal securitiesLendingBalance;
        private BigDecimal securitiesLendingBalanceAmount;
        private BigDecimal securitiesLendingSellVolume;
        private BigDecimal securitiesLendingRepayVolume;
        private BigDecimal marginBalance;
        private String extra;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndustrySummaryDTO {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountStatsDTO {
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
        private OffsetDateTime updatedAt;
    }
}
