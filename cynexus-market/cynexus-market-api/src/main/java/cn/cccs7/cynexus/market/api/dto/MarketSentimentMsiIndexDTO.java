package cn.cccs7.cynexus.market.api.dto;

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
public class MarketSentimentMsiIndexDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private ComponentsDTO components;
    private WeightsDTO weights;
    private BigDecimal compositeScore;
    private Integer componentCount;
    private String level;
    private Integer elapsedMs;
    private String source;
    private Boolean fromCache;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentsDTO {
        private BigDecimal vol;
        private BigDecimal turnover;
        private BigDecimal priceStrength;
        private BigDecimal riskAppetite;
        private BigDecimal breadth;
        private BigDecimal limitEmotion;
        private BigDecimal profitEffect;
        private BigDecimal sectorBreadth;
        private BigDecimal styleRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightsDTO {
        private BigDecimal vol;
        private BigDecimal turnover;
        private BigDecimal priceStrength;
        private BigDecimal riskAppetite;
        private BigDecimal breadth;
        private BigDecimal limitEmotion;
        private BigDecimal profitEffect;
        private BigDecimal sectorBreadth;
        private BigDecimal styleRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private BigDecimal compositeScore;
        private String level;
        private Integer componentCount;
        private ComponentsDTO components;
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
