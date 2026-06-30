package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class IndustryApplicationBO {

    private IndustryApplicationBO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private Integer version;
        private OffsetDateTime updatedAt;
        private Integer horizonDays;
        private Integer horizonSegments;
        private List<Target> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private Long id;
        private String targetKey;
        private String targetType;
        private String symbol;
        private String name;
        private Boolean enabled;
        private Integer intervalMinutes;
        private List<String> tags;
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetCode {
        private String code;
        private String name;
        private String kind;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kline {
        private String targetType;
        private String code;
        private String name;
        private String period;
        private List<Bar> bars;
        private Indicators indicators;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bar {
        private String time;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal prevClose;
        private BigDecimal pct;
        private Long volumeLots;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Indicators {
        private BigDecimal latestClose;
        private BigDecimal latestPct;
        private String latestTime;
        private BigDecimal ma20;
        private BigDecimal ma60;
        private BigDecimal ma120;
        private BigDecimal ma250;
        private Boolean aboveMa20;
        private Boolean aboveMa60;
        private BigDecimal aboveMa20Pct;
        private BigDecimal aboveMa60Pct;
        private Integer aboveMa20Streak;
        private BigDecimal high20;
        private BigDecimal low20;
        private BigDecimal rangePos20;
        private BigDecimal return5d;
        private BigDecimal return20d;
        private BigDecimal return60d;
        private Integer barCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private Target target;
        private OffsetDateTime updatedAt;
        private List<Bar> bars;
        private Indicators indicators;
        private String source;
        private String period;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorOverviewItem {
        private String fullCode;
        private String name;
        private String kind;
        private BigDecimal lastPrice;
        private BigDecimal preClosePrice;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal change;
        private BigDecimal changePct;
        private BigDecimal amplitudePct;
        private BigDecimal highPct;
        private BigDecimal lowPct;
        private BigDecimal openPct;
        private Long volume;
        private BigDecimal amount;
        private LocalDate tradingDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowSnapshot {
        private Boolean ok;
        private LocalDate tradeDate;
        private Integer rowCount;
        private Integer totalPages;
        private List<Integer> pageRowCounts;
        private OffsetDateTime fetchedAt;
        private List<FundFlowRow> rows;
        private String source;
        private Boolean stale;
        private String staleReason;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowHistory {
        private Boolean ok;
        private Integer days;
        private List<LocalDate> dates;
        private Integer rowCount;
        private OffsetDateTime fetchedAt;
        private List<FundFlowRow> rows;
        private String source;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowRow {
        private Integer rank;
        private String industry;
        private String code;
        private BigDecimal changePct;
        private BigDecimal inflow;
        private BigDecimal outflow;
        private BigDecimal net;
        private Integer companyCount;
        private String leaderStock;
        private BigDecimal leaderChange;
        private BigDecimal leaderPrice;
        private LocalDate tradeDate;
        private String source;
    }
}
