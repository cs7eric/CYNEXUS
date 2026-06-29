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
public class MarketSentimentLimitEmotionSummaryDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private LocalDate prevTradeDate;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer touchedCount;
    private Integer brokenCount;
    private BigDecimal breakBoardRate;
    private BigDecimal limitUpDownRatio;
    private Integer yesterdayLimitUpCount;
    private BigDecimal yesterdayLimitUpAvgReturn;
    private ComponentsDTO components;
    private BigDecimal compositeScore;
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
        private BigDecimal upDownScore;
        private BigDecimal breakBoardScore;
        private BigDecimal yesterdayReturnScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private Integer limitUpCount;
        private Integer limitDownCount;
        private Integer touchedCount;
        private Integer brokenCount;
        private BigDecimal breakBoardRate;
        private BigDecimal limitUpDownRatio;
        private Integer yesterdayLimitUpCount;
        private BigDecimal yesterdayLimitUpAvgReturn;
        private BigDecimal compositeScore;
        private String level;
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
