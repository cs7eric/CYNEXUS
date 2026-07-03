package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class IndustryApplicationDTO {

    private IndustryApplicationDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigResponse {
        private Integer version;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        private Horizon horizon;
        private List<Target> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Horizon {
        private Integer days;
        private Integer segments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Target {
        private String id;
        @JsonProperty("target_type")
        private String targetType;
        private String symbol;
        private String name;
        private Boolean enabled;
        @JsonProperty("interval_minutes")
        private Integer intervalMinutes;
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetCodesResponse {
        private List<TargetCode> items;
        private Integer count;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetCode {
        private String code;
        private String name;
        private String kind;
        @JsonProperty("tdx_fund_flow_supported")
        private Boolean tdxFundFlowSupported;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KlineResponse {
        @JsonProperty("target_type")
        private String targetType;
        private String code;
        private String name;
        private String period;
        private List<Bar> kline;
        private Indicators indicators;
        @JsonProperty("fetched_at")
        private OffsetDateTime fetchedAt;
        private String source;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bar {
        private String time;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        @JsonProperty("prev_close")
        private BigDecimal prevClose;
        private BigDecimal pct;
        @JsonProperty("volume_lots")
        private Long volumeLots;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Indicators {
        @JsonProperty("latest_close")
        private BigDecimal latestClose;
        @JsonProperty("latest_pct")
        private BigDecimal latestPct;
        @JsonProperty("latest_time")
        private String latestTime;
        private BigDecimal ma20;
        private BigDecimal ma60;
        private BigDecimal ma120;
        private BigDecimal ma250;
        @JsonProperty("above_ma20")
        private Boolean aboveMa20;
        @JsonProperty("above_ma60")
        private Boolean aboveMa60;
        @JsonProperty("above_ma20_pct")
        private BigDecimal aboveMa20Pct;
        @JsonProperty("above_ma60_pct")
        private BigDecimal aboveMa60Pct;
        @JsonProperty("above_ma20_streak")
        private Integer aboveMa20Streak;
        @JsonProperty("high_20")
        private BigDecimal high20;
        @JsonProperty("low_20")
        private BigDecimal low20;
        @JsonProperty("range_pos_20")
        private BigDecimal rangePos20;
        @JsonProperty("return_5d")
        private BigDecimal return5d;
        @JsonProperty("return_20d")
        private BigDecimal return20d;
        @JsonProperty("return_60d")
        private BigDecimal return60d;
        @JsonProperty("bar_count")
        private Integer barCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshResponse {
        private Boolean ok;
        private List<RefreshItem> items;
        private Integer count;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshItem {
        private String id;
        private Boolean ok;
        @JsonProperty("kline_count")
        private Integer klineCount;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultResponse {
        private Target target;
        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
        private List<Bar> kline;
        private Indicators indicators;
        private ResultMeta meta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultMeta {
        private String source;
        private String period;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewResponse {
        private Boolean ok;
        private List<SectorOverviewItem> items;
        @JsonProperty("industry_count")
        private Integer industryCount;
        @JsonProperty("concept_count")
        private Integer conceptCount;
        @JsonProperty("fetched_at")
        private OffsetDateTime fetchedAt;
        private String source;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorOverviewItem {
        @JsonProperty("full_code")
        private String fullCode;
        private String name;
        private String kind;
        @JsonProperty("last_price")
        private BigDecimal lastPrice;
        @JsonProperty("pre_close_price")
        private BigDecimal preClosePrice;
        @JsonProperty("open_price")
        private BigDecimal openPrice;
        @JsonProperty("high_price")
        private BigDecimal highPrice;
        @JsonProperty("low_price")
        private BigDecimal lowPrice;
        private BigDecimal change;
        @JsonProperty("change_pct")
        private BigDecimal changePct;
        @JsonProperty("amplitude_pct")
        private BigDecimal amplitudePct;
        @JsonProperty("high_pct")
        private BigDecimal highPct;
        @JsonProperty("low_pct")
        private BigDecimal lowPct;
        @JsonProperty("open_pct")
        private BigDecimal openPct;
        private Long volume;
        private BigDecimal amount;
        @JsonProperty("trading_date")
        private String tradingDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowResponse {
        private Boolean ok;
        private String tradeDate;
        private String requestedTradeDate;
        private Boolean fallbackTradeDate;
        private String servingMode;
        private Integer rowCount;
        private Integer totalPages;
        private List<Integer> pageRowCounts;
        private OffsetDateTime fetchedAt;
        private List<FundFlowRow> rows;
        private String source;
        private String sourceKind;
        private Boolean stale;
        private String staleReason;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundFlowHistoryResponse {
        private Boolean ok;
        private Integer days;
        private List<String> dates;
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
        @JsonProperty("序号")
        private Integer rank;
        @JsonProperty("行业")
        private String industry;
        private String code;
        @JsonProperty("行业指数涨跌幅")
        private BigDecimal changePct;
        @JsonProperty("流入资金(亿)")
        private BigDecimal inflow;
        @JsonProperty("流出资金(亿)")
        private BigDecimal outflow;
        @JsonProperty("净额(亿)")
        private BigDecimal net;
        @JsonProperty("公司家数")
        private Integer companyCount;
        @JsonProperty("领涨股")
        private String leaderStock;
        @JsonProperty("领涨股涨跌幅")
        private BigDecimal leaderChange;
        @JsonProperty("当前价(元)")
        private BigDecimal leaderPrice;
        private String tradeDate;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxFundFlowResponse {
        private Boolean applicable;
        private Boolean ok;
        @JsonProperty("target_type")
        private String targetType;
        private String symbol;
        @JsonProperty("requested_trade_date")
        private String requestedTradeDate;
        @JsonProperty("trade_date")
        private String tradeDate;
        private String code6;
        @JsonProperty("full_code")
        private String fullCode;
        @JsonProperty("tdx_name")
        private String tdxName;
        @JsonProperty("flow_sector_name")
        private String flowSectorName;
        @JsonProperty("seed_code")
        private String seedCode;
        @JsonProperty("flow_trade_date")
        private String flowTradeDate;
        @JsonProperty("snapshot_status")
        private String snapshotStatus;
        @JsonProperty("selection_mode")
        private String selectionMode;
        @JsonProperty("name_match_type")
        private String nameMatchType;
        @JsonProperty("main_net")
        private BigDecimal mainNet;
        @JsonProperty("large_order_net")
        private BigDecimal largeOrderNet;
        @JsonProperty("medium_order_net")
        private BigDecimal mediumOrderNet;
        @JsonProperty("small_order_net")
        private BigDecimal smallOrderNet;
        @JsonProperty("row_count")
        private Integer rowCount;
        @JsonProperty("history_days")
        private Integer historyDays;
        private List<TdxFundFlowPoint> history;
        @JsonProperty("ths_mappings")
        private List<TdxThsIndustryMapping> thsMappings;
        private List<TdxFundFlowConstituent> constituents;
        private String source;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxFundFlowConstituent {
        private String code;
        private String name;
        private BigDecimal price;
        @JsonProperty("change_pct")
        private BigDecimal changePct;
        @JsonProperty("turnover_pct")
        private BigDecimal turnoverPct;
        @JsonProperty("amount_text")
        private String amountText;
        private List<String> sources;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxThsIndustryMapping {
        @JsonProperty("tdx_code6")
        private String tdxCode6;
        @JsonProperty("tdx_name")
        private String tdxName;
        @JsonProperty("ths_code")
        private String thsCode;
        @JsonProperty("ths_name")
        private String thsName;
        @JsonProperty("match_type")
        private String matchType;
        private BigDecimal confidence;
        private Integer priority;
        @JsonProperty("is_primary")
        private Boolean isPrimary;
        private String remark;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxFundFlowPoint {
        @JsonProperty("trade_date")
        private String tradeDate;
        @JsonProperty("flow_trade_date")
        private String flowTradeDate;
        @JsonProperty("snapshot_status")
        private String snapshotStatus;
        @JsonProperty("selection_mode")
        private String selectionMode;
        @JsonProperty("main_net")
        private BigDecimal mainNet;
        @JsonProperty("large_order_net")
        private BigDecimal largeOrderNet;
        @JsonProperty("medium_order_net")
        private BigDecimal mediumOrderNet;
        @JsonProperty("small_order_net")
        private BigDecimal smallOrderNet;
    }
}
