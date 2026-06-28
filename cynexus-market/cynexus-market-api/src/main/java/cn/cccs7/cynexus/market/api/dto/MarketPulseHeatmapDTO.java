package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class MarketPulseHeatmapDTO {

    private Boolean ok;
    private String kind;
    private String source;
    private Integer count;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemDTO {
        private String code;
        private String name;
        private BigDecimal value;
        @JsonProperty("change_pct")
        @JsonAlias({"changePercent", "changePct"})
        private BigDecimal changePct;
        private BigDecimal changePercent;
        private String kind;
        private String fullCode;
        private String sectorCode;
        private String kindLabel;
        private Integer validSize;
        private Integer stockCount;
        private Integer risingCount;
        private Integer fallingCount;
        private Integer flatCount;
        private Integer limitUpCount;
        @JsonAlias("stockCount")
        private Integer count;
        private BigDecimal amount;
        private BigDecimal mainNetInflow;
        private BigDecimal circulatingMarketCap;
        private BigDecimal turnoverRateAvg;
        private BigDecimal speedAvg;
        private Integer limitStreakCount;
        private List<String> conceptTags;
        private List<StockDTO> children;
        private String industry;
        private List<String> concepts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockDTO {
        private String code;
        private String name;
        private String fullCode;
        private BigDecimal latestPrice;
        private BigDecimal changePercent;
        private BigDecimal amount;
        private BigDecimal volume;
        private BigDecimal turnoverRate;
        private BigDecimal circulatingMarketCap;
        private BigDecimal totalMarketCap;
        private BigDecimal mainNetInflow;
        private BigDecimal speed;
        private Integer limitStreak;
        private BigDecimal boardSealedAmount;
        private List<String> conceptTags;
        private Boolean isLimitUp;
        private String sectorCode;
        private String sectorName;
    }
}
