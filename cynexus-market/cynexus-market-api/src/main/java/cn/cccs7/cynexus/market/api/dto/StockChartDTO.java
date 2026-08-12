package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
    public static class StockSectorEntryDTO {
        @JsonProperty("sector_id")
        private String sectorId;
        private String name;
        @JsonProperty("topic_id")
        private String topicId;
        @JsonProperty("category_raw")
        private Integer categoryRaw;
        private BigDecimal changePercent;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSectorsResponseDTO {
        private String code;
        private List<StockSectorEntryDTO> industries;
        private List<StockSectorEntryDTO> concepts;
        private List<StockSectorEntryDTO> styles;
        private Integer count;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorMetaDTO {
        @JsonProperty("sector_id")
        private String sectorId;
        private String name;
        @JsonProperty("category_raw")
        private Integer categoryRaw;
        @JsonProperty("category_label")
        private String categoryLabel;
        @JsonProperty("topic_id")
        private String topicId;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorStockDTO {
        private String symbol;
        private String name;
        private String market;
        @JsonProperty("relation_level")
        private BigDecimal relationLevel;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        @JsonProperty("last_price")
        private BigDecimal lastPrice;
        @JsonProperty("pre_close")
        private BigDecimal preClose;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal change;
        @JsonProperty("change_pct")
        private BigDecimal changePct;
        @JsonProperty("volume_lots")
        private BigDecimal volumeLots;
        @JsonProperty("amount_wan")
        private BigDecimal amountWan;
        @JsonProperty("turnover_rate")
        private BigDecimal turnoverRate;
        private BigDecimal amplitude;
        @JsonProperty("total_market_cap")
        private BigDecimal totalMarketCap;
        @JsonProperty("float_market_cap")
        private BigDecimal floatMarketCap;
        @JsonProperty("return_pct_5d")
        private BigDecimal returnPct5d;
        @JsonProperty("return_pct_10d")
        private BigDecimal returnPct10d;
        @JsonProperty("return_pct_30d")
        private BigDecimal returnPct30d;
        @JsonProperty("return_pct_60d")
        private BigDecimal returnPct60d;
        @JsonProperty("return_pct_252d")
        private BigDecimal returnPct252d;
        @JsonProperty("captured_at")
        private OffsetDateTime capturedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorStocksResponseDTO {
        private SectorMetaDTO sector;
        private List<SectorStockDTO> stocks;
        private Integer count;
        private String source;
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
    public static class AuctionDTO {
        private String symbol;
        @JsonProperty("trade_date")
        private LocalDate tradeDate;
        private String source;
        private String error;
        private AuctionPhaseDTO opening;
        private AuctionPhaseDTO closing;
        private AuctionDetailsDTO details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionHistoryResponseDTO {
        private String symbol;
        private String source;
        private List<AuctionDTO> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionPhaseDTO {
        private BigDecimal price;
        private BigDecimal volume;
        private BigDecimal amount;
        private BigDecimal matchPrice;
        private BigDecimal unmatchedBuyVolume;
        private BigDecimal unmatchedSellVolume;
        private String time;
        private BigDecimal unmatchedDelta;
        private String strengthLabel;
        private Boolean anchorExact;
        private String anchorSource;
        private String anchorTargetTime;
        private AuctionPriceRangeDTO priceRange;
        private String recentPriceTrend;
        private BigDecimal recentPriceChange;
        private BigDecimal recentVolumeDelta;
        private String directionStability;
        private Integer directionFlipCount;
        private String dominantDirection;
        private BigDecimal imbalancePressure;
        private String dataConfidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionPriceRangeDTO {
        private BigDecimal low;
        private BigDecimal high;
        private BigDecimal spread;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionDetailsDTO {
        private Map<String, Object> quote;
        private Map<String, Object> auction0925;
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
        @JsonProperty("unmatched_direction_raw")
        private Integer unmatchedDirectionRaw;
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
        @JsonProperty("pre_close")
        private BigDecimal preClose;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10ValuationRowDTO {
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
    public static class F10ValuationDTO {
        private String symbol;
        private String reqId;
        private List<F10ValuationRowDTO> rows;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10FinanceReportRowDTO {
        private String rq;
        private Map<String, Object> fields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10FinanceReportDTO {
        private String symbol;
        private String reportType;
        private List<F10FinanceReportRowDTO> rows;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10BusinessCompositionItemDTO {
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
    public static class F10BusinessCompositionDTO {
        private String symbol;
        private String reportDate;
        private List<F10BusinessCompositionItemDTO> items;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10ProfitForecastItemDTO {
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
    public static class F10ProfitForecastDTO {
        private String symbol;
        private List<F10ProfitForecastItemDTO> items;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10AnnouncementItemDTO {
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
    public static class F10AnnouncementsDTO {
        private String symbol;
        private List<F10AnnouncementItemDTO> items;
        private Integer count;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10NewsItemDTO {
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
    public static class F10NewsDTO {
        private String symbol;
        private List<F10NewsItemDTO> items;
        private Integer count;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10RoadshowItemDTO {
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
    public static class F10RoadshowsDTO {
        private String symbol;
        private List<F10RoadshowItemDTO> items;
        private Integer count;
        private String fetchedAt;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class F10CompanyNewsItemDTO {
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
    public static class F10CompanyNewsDTO {
        private String symbol;
        private String section;
        private List<F10CompanyNewsItemDTO> items;
        private Integer count;
        private String fetchedAt;
        private String source;
    }
}
