package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketPulseStyleSectorConstituentsDTO {

    private Boolean ok;
    private String error;
    private String name;
    private String code;
    private Integer count;
    private Integer matched;
    private String indexFetchedAt;
    private String rowsFetchedAt;
    private String dataSource;
    private Boolean isTradingDay;
    private Boolean isMarketOpen;
    private String tradingHoursMode;
    private LocalDate snapshotDate;
    private List<IndustryRowDTO> rows;
    private List<String> codes;
    private List<ConstituentDTO> constituents;
    @JsonProperty("sample_size")
    private Integer sampleSize;
    @JsonProperty("valid_size")
    private Integer validSize;
    @JsonProperty("change_pct")
    private BigDecimal changePct;
    @JsonProperty("fetched_at")
    private String fetchedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConstituentDTO {
        private String code;
        private String name;
        private Boolean valid;
        private BigDecimal price;
        @JsonProperty("last_price")
        private BigDecimal lastPrice;
        private BigDecimal changePct;
        @JsonProperty("change_pct")
        private BigDecimal changePctRaw;
        @JsonProperty("change_amount")
        private BigDecimal changeAmount;
        @JsonProperty("turnover_rate")
        private BigDecimal turnoverRate;
        private BigDecimal amplitude;
        @JsonProperty("turnover_amount")
        private BigDecimal turnoverAmount;
        @JsonProperty("circulating_shares")
        private BigDecimal circulatingShares;
        @JsonProperty("circulating_market_cap")
        private BigDecimal circulatingMarketCap;
        private BigDecimal amount;
        private String industry;
        private List<String> concepts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryRowDTO {
        @JsonProperty("序号")
        private Integer rank;
        @JsonProperty("代码")
        private String code;
        @JsonProperty("名称")
        private String name;
        @JsonProperty("现价")
        private BigDecimal price;
        @JsonProperty("涨跌幅(%)")
        private BigDecimal changePct;
        @JsonProperty("涨跌")
        private BigDecimal change;
        @JsonProperty("涨速(%)")
        private BigDecimal speedPct;
        @JsonProperty("换手(%)")
        private BigDecimal turnoverPct;
        @JsonProperty("量比")
        private BigDecimal volumeRatio;
        @JsonProperty("振幅(%)")
        private BigDecimal amplitudePct;
        @JsonProperty("成交额")
        private String amount;
        @JsonProperty("流通股")
        private String floatShares;
        @JsonProperty("流通市值")
        private String floatMarketCap;
        @JsonProperty("市盈率")
        private BigDecimal pe;
    }
}
