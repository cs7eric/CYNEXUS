package cn.cccs7.cynexus.market.api.dto;

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
public class MarketSentimentRiskAppetiteDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private Integer windowDays;
    private AssetDTO hs300;
    private TreasuryDTO treasury;
    private SpreadDTO spread;
    private BigDecimal score;
    private BigDecimal rawValue;
    private Integer elapsedMs;
    private String source;
    private Boolean fromCache;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetDTO {
        private BigDecimal close;
        private LocalDate currentDate;
        private BigDecimal baseClose;
        private LocalDate baseDate;
        private BigDecimal returnPct;
        private BigDecimal weight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreasuryDTO {
        @JsonProperty("511010")
        private AssetDTO asset511010;
        @JsonProperty("511090")
        private AssetDTO asset511090;
        private WeightedDTO weighted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightedDTO {
        private BigDecimal returnPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpreadDTO {
        @JsonProperty("511010")
        private BigDecimal spread511010;
        @JsonProperty("511090")
        private BigDecimal spread511090;
        private BigDecimal weighted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private BigDecimal hs300ReturnPct;
        private BigDecimal treasury511010ReturnPct;
        private BigDecimal treasury511090ReturnPct;
        private BigDecimal treasuryWeightedReturnPct;
        private BigDecimal spread511010;
        private BigDecimal spread511090;
        private BigDecimal spreadWeighted;
        private BigDecimal score;
        private Boolean fromCache;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResponseDTO {
        private Boolean ok;
        private LocalDate start;
        private LocalDate end;
        private Integer count;
        private List<HistoryItemDTO> items;
        private String error;
    }
}
