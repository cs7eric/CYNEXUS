package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StockScreenerDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean ok;
        private String requestedTradeDate;
        private String tradeDate;
        private String universe;
        private boolean excludeSt;
        private boolean excludeChiNext;
        private boolean excludeStar;
        private String industry;
        private String tdxIndustryCode;
        private String thsIndustryCode;
        private String conceptSectorId;
        private String styleSectorId;
        private String board;
        private String sort;
        private String sortDir;
        private int page;
        private int pageSize;
        private int universeCount;
        private int count;
        private String source;
        private List<Item> items;
        private List<String> warnings;
        private String error;
        private DataQuality dataQuality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String code;
        private String name;
        private String market;
        private String industry;
        private String board;
        private boolean st;
        private BigDecimal price;
        private BigDecimal changePct;
        private BigDecimal amplitudePct;
        private BigDecimal amount;
        private BigDecimal totalMarketCap;
        private BigDecimal floatMarketCap;
        private BigDecimal returnPct5d;
        private BigDecimal returnPct10d;
        private BigDecimal returnPct30d;
        private BigDecimal returnPct60d;
        private BigDecimal returnPct252d;
        private BigDecimal mainNet;
        private BigDecimal mainNet5d;
        private String tdxIndustryCode;
        private String tdxIndustryName;
        private String thsIndustryCode;
        private String thsIndustryName;
        private List<SectorMembership> concepts;
        private List<SectorMembership> styles;
        private BigDecimal turnoverPct;
        private BigDecimal volumeRatio;
        private BigDecimal pe;
        private PriceVolumePatternMetrics priceVolumePattern;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceVolumePatternMetrics {
        private Integer observedContractionDays;
        private BigDecimal breakoutVolumeMultiple;
        private BigDecimal bullishBodyPct;
        private BigDecimal closeAboveMa20Pct;
        private BigDecimal ma5AboveMa20Pct;
        private Integer breakoutHighDays;
        private BigDecimal atrPct20;
        private Integer consecutiveBullishDays;
        private BigDecimal volumeVsMa5Multiple;
        private List<String> candlestickPatterns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQuality {
        private String status;
        private String snapshotDate;
        private long freshnessLagDays;
        private int eligibleStockCount;
        private int dailySnapshotCount;
        private List<FieldCoverage> fieldCoverage;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldCoverage {
        private String field;
        private int availableCount;
        private int eligibleCount;
        private BigDecimal coveragePct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndustryOption {
        private String code;
        private String name;
        private int stockCount;
        private String snapshotDate;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationOptions {
        private List<IndustryOption> tdxIndustryGroups;
        private List<IndustryOption> thsIndustries;
        private List<IndustryOption> concepts;
        private List<IndustryOption> styles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMembership {
        private String sectorId;
        private String name;
    }
}
