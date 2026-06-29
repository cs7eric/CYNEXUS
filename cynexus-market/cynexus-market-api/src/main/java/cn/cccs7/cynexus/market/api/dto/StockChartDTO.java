package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class StockChartDTO {

    private StockChartDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItemDTO {
        @JsonProperty("target_type")
        private String targetType;
        private String symbol;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponseDTO {
        private List<SearchItemDTO> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KlineBarDTO {
        private Long timestamp;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal volume;
        private BigDecimal turnover;
        @JsonProperty("turnover_rate")
        private BigDecimal turnoverRate;
        @JsonProperty("volume_ratio")
        private BigDecimal volumeRatio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KlineResponseDTO {
        private String symbol;
        @JsonProperty("target_type")
        private String targetType;
        private String period;
        private String adjust;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        private String source;
        private List<KlineBarDTO> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMetaDTO {
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
    public static class FundFlowSnapshotDTO {
        private Boolean ok;
        private String symbol;
        private String name;
        @JsonProperty("trade_date")
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
        @JsonProperty("captured_at")
        private OffsetDateTime capturedAt;
        private String error;
        private List<FundFlowPointDTO> history;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowPointDTO {
        @JsonProperty("trade_date")
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
    public static class AuctionDTO {
        private String symbol;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        private AuctionPhaseDTO opening;
        private AuctionPhaseDTO closing;
        private AuctionDetailsDTO details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionPhaseDTO {
        private BigDecimal price;
        private BigDecimal volume;
        private BigDecimal amount;
        private String time;
        private String dataConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionDetailsDTO {
        private List<AuctionPointDTO> openingPoints;
        private List<AuctionPointDTO> closingPoints;
        private List<AuctionPointDTO> allPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionPointDTO {
        @JsonProperty("time_label")
        private String timeLabel;
        private BigDecimal price;
        @JsonProperty("matched_volume")
        private BigDecimal matchedVolume;
        @JsonProperty("unmatched_volume")
        private BigDecimal unmatchedVolume;
        @JsonProperty("matched_amount_estimated")
        private BigDecimal matchedAmountEstimated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntradayDTO {
        private Boolean ok;
        private String symbol;
        @JsonProperty("target_type")
        private String targetType;
        private String name;
        private String adjust;
        private String effectiveAdjust;
        private String requestedAdjust;
        private String source;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        @JsonProperty("requested_trade_date")
        private LocalDate requestedTradeDate;
        private List<IntradayPointDTO> timeshare;
        @JsonProperty("minute_bars")
        private MinuteBarsDTO minuteBars;
        private String error;

        @JsonProperty("effective_adjust")
        public String getEffectiveAdjust() {
            return effectiveAdjust;
        }

        @JsonProperty("requested_adjust")
        public String getRequestedAdjust() {
            return requestedAdjust;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntradayPointDTO {
        private Long timestamp;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        @JsonProperty("time_label")
        private String timeLabel;
        private BigDecimal price;
        private BigDecimal open;
        @JsonProperty("avg_price")
        private BigDecimal avgPrice;
        private BigDecimal volume;
        private BigDecimal turnover;
        @JsonProperty("turnover_rate")
        private BigDecimal turnoverRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinuteBarsDTO {
        @JsonProperty("1m")
        private List<KlineBarDTO> oneMinute;
        @JsonProperty("5m")
        private List<KlineBarDTO> fiveMinute;
        @JsonProperty("15m")
        private List<KlineBarDTO> fifteenMinute;
        @JsonProperty("30m")
        private List<KlineBarDTO> thirtyMinute;
        @JsonProperty("60m")
        private List<KlineBarDTO> sixtyMinute;
    }
}
