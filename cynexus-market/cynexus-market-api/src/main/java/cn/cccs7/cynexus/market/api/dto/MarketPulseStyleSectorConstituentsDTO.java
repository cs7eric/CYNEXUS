package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
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
}
