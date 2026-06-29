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
        private BigDecimal activeNetAmountWan;
        private BigDecimal activeBuyRatio;
        private BigDecimal activeSellRatio;
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
}
