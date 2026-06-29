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
public class MarketSentimentVolatilityDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private String underlyingCode;
    private String underlyingName;
    private BigDecimal close;
    private BigDecimal dailyReturnPct;
    private BigDecimal realizedVol20d;
    private Integer volWindowDays;
    private Integer volLookbackDays;
    private BigDecimal percentile1y;
    private BigDecimal score;
    private BigDecimal sentimentScore;
    private Integer sampleCount;
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
        private String underlyingCode;
        private String underlyingName;
        private BigDecimal close;
        private BigDecimal dailyReturnPct;
        private BigDecimal realizedVol20d;
        private Integer volWindowDays;
        private Integer volLookbackDays;
        private BigDecimal percentile1y;
        private BigDecimal score;
        private BigDecimal sentimentScore;
        private Integer sampleCount;
        private Integer elapsedMs;
        private String source;
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
