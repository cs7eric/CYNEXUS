package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class StockChartBO {

    private StockChartBO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {
        private String targetType;
        private String symbol;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bar {
        private Long timestamp;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal turnover;
        private BigDecimal turnoverRate;
        private BigDecimal volumeRatio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMeta {
        private String symbol;
        private String name;
        private BigDecimal totalMarketCap;
        private BigDecimal circMarketCap;
        private String industry;
        private String capStyle;
        private String sectorIndexSymbol;
        private String sectorIndexName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorEntry {
        private String sectorId;
        private String name;
        private String topicId;
        private Integer categoryRaw;
        private BigDecimal changePercent;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSectors {
        private String code;
        private List<SectorEntry> industries;
        private List<SectorEntry> concepts;
        private List<SectorEntry> styles;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMeta {
        private String sectorId;
        private String name;
        private Integer categoryRaw;
        private String categoryLabel;
        private String topicId;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorStock {
        private String symbol;
        private String name;
        private String market;
        private BigDecimal relationLevel;
        private LocalDate tradeDate;
        private BigDecimal lastPrice;
        private BigDecimal preClose;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal change;
        private BigDecimal changePct;
        private BigDecimal volumeLots;
        private BigDecimal amountWan;
        private BigDecimal turnoverRate;
        private BigDecimal amplitude;
        private BigDecimal totalMarketCap;
        private BigDecimal floatMarketCap;
        private BigDecimal returnPct5d;
        private BigDecimal returnPct10d;
        private BigDecimal returnPct30d;
        private BigDecimal returnPct60d;
        private BigDecimal returnPct252d;
        private OffsetDateTime capturedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorStocks {
        private SectorMeta sector;
        private List<SectorStock> stocks;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowSnapshot {
        private Boolean ok;
        private String symbol;
        private String name;
        private LocalDate tradeDate;
        private BigDecimal lastPrice;
        private BigDecimal changePct;
        private BigDecimal amountWan;
        private BigDecimal turnoverRate;
        private BigDecimal activeNetAmountWan;
        private BigDecimal mainNetAmountWan;
        private BigDecimal superNetAmountWan;
        private BigDecimal largeNetAmountWan;
        private BigDecimal mediumNetAmountWan;
        private BigDecimal smallNetAmountWan;
        private BigDecimal activeBuyRatio;
        private BigDecimal activeSellRatio;
        private BigDecimal buyBigRatio;
        private BigDecimal sellBigRatio;
        private String source;
        private OffsetDateTime capturedAt;
        private String error;
        private List<FundFlowPoint> history;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowPoint {
        private LocalDate tradeDate;
        private BigDecimal lastPrice;
        private BigDecimal turnoverRate;
        private BigDecimal activeNetAmountWan;
        private BigDecimal mainNetAmountWan;
        private BigDecimal superNetAmountWan;
        private BigDecimal largeNetAmountWan;
        private BigDecimal mediumNetAmountWan;
        private BigDecimal smallNetAmountWan;
        private BigDecimal activeBuyRatio;
        private BigDecimal activeSellRatio;
        private BigDecimal buyBigRatio;
        private BigDecimal sellBigRatio;
        private BigDecimal amountWan;
        private BigDecimal changePct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntradayPoint {
        private Long timestamp;
        private LocalDate tradeDate;
        private String timeLabel;
        private BigDecimal price;
        private BigDecimal open;
        private BigDecimal avgPrice;
        private BigDecimal volume;
        private BigDecimal turnover;
        private BigDecimal turnoverRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intraday {
        private Boolean ok;
        private String symbol;
        private String targetType;
        private String name;
        private String adjust;
        private String source;
        private LocalDate tradeDate;
        private LocalDate requestedTradeDate;
        private List<IntradayPoint> timeshare;
        private Map<String, List<Bar>> minuteBars;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionPoint {
        private Long timestamp;
        private LocalDate tradeDate;
        private String timeLabel;
        private String phase;
        private BigDecimal price;
        private BigDecimal matchedVolume;
        private BigDecimal matchedAmount;
        private BigDecimal unmatchedBuyVolume;
        private BigDecimal unmatchedSellVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10ValuationRow {
        private String date;
        private BigDecimal peTtm;
        private BigDecimal peBfw;
        private BigDecimal pbMrq;
        private BigDecimal pbBfw;
        private BigDecimal pcfOcfTtm;
        private BigDecimal pcfBfw;
        private BigDecimal psTtm;
        private BigDecimal psBfw;
        private BigDecimal peg;
        private BigDecimal avgMarketCap;
        private BigDecimal aliqMarketCap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10Valuation {
        private String symbol;
        private String reqId;
        private List<F10ValuationRow> rows;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10FinanceReportRow {
        private String rq;
        private Map<String, Object> fields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10FinanceReport {
        private String symbol;
        private String reportType;
        private List<F10FinanceReportRow> rows;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10BusinessCompositionItem {
        private String category;
        private String subType;
        private String name;
        private BigDecimal revenue;
        private BigDecimal ratio;
        private List<Object> extras;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10BusinessComposition {
        private String symbol;
        private String reportDate;
        private List<F10BusinessCompositionItem> items;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10ProfitForecastItem {
        private String year;
        private BigDecimal netProfit;
        private BigDecimal eps;
        private BigDecimal revenue;
        private List<Object> extras;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10ProfitForecast {
        private String symbol;
        private List<F10ProfitForecastItem> items;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10AnnouncementItem {
        private String issueDate;
        private String title;
        private String typecode;
        private String typename;
        private String recId;
        private String tableid;
        private String url;
        private String redistime;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10Announcements {
        private String symbol;
        private List<F10AnnouncementItem> items;
        private Integer count;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10NewsItem {
        private String issueDate;
        private String title;
        private String recId;
        private String tableid;
        private String redistime;
        private String source;
        private String relatecolumn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10News {
        private String symbol;
        private List<F10NewsItem> items;
        private Integer count;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10RoadshowItem {
        private String title;
        private String roadshowType;
        private String startDate;
        private String startTime;
        private String endTime;
        private String summary;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10Roadshows {
        private String symbol;
        private List<F10RoadshowItem> items;
        private Integer count;
        private OffsetDateTime fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10CompanyNewsItem {
        private String rating;
        private String analysts;
        private String recId;
        private String issueDate;
        private String title;
        private String nflag;
        private String docHash;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10CompanyNews {
        private String symbol;
        private String section;
        private List<F10CompanyNewsItem> items;
        private Integer count;
        private OffsetDateTime fetchedAt;
        private String source;
    }
}
