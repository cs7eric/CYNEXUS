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
public class MarketSentimentMaCountDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private Integer totalEligible;
    private Integer aboveMa20;
    private Integer aboveMa60;
    private Integer aboveBoth;
    private BigDecimal pctAboveMa20;
    private BigDecimal pctAboveMa60;
    private BigDecimal pctAboveBoth;
    private Integer up5dCount;
    private BigDecimal pctUp5d;
    private Integer newLow60dCount;
    private BigDecimal pctNewLow60d;
    private Integer newHigh252dCount;
    private BigDecimal pctNewHigh252d;
    private Integer advancingCount;
    private BigDecimal pctAdvancing;
    private BigDecimal newHigh252dScore;
    private BigDecimal newHigh252dRawValue;
    private BigDecimal breadthScore;
    private BigDecimal breadthRawValue;
    private Integer elapsedMs;
    private String source;
    private Boolean fromCache;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private Integer totalEligible;
        private Integer aboveMa20;
        private Integer aboveMa60;
        private Integer aboveBoth;
        private BigDecimal pctAboveMa20;
        private BigDecimal pctAboveMa60;
        private BigDecimal pctAboveBoth;
        private Integer up5dCount;
        private BigDecimal pctUp5d;
        private Integer newLow60dCount;
        private BigDecimal pctNewLow60d;
        private Integer newHigh252dCount;
        private BigDecimal pctNewHigh252d;
        private Integer advancingCount;
        private BigDecimal pctAdvancing;
        private BigDecimal newHigh252dScore;
        private BigDecimal breadthScore;
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
