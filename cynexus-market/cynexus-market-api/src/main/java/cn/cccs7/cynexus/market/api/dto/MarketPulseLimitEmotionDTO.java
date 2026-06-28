package cn.cccs7.cynexus.market.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class MarketPulseLimitEmotionDTO {

    private Boolean ok;
    private String error;
    private LimitBlockDTO limitUp;
    private LimitBlockDTO limitDown;
    private BreakBoardDTO breakBoard;
    private StreakDTO streak;
    private LocalDate tradeDate;
    private String updateTime;
    private String marketStatus;
    private String dataStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LimitBlockDTO {
        private Integer count;
        private String label;
        private List<StockDTO> stocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockDTO {
        private String code;
        private String name;
        private Integer streak;
        private Integer previousStreak;
        private BigDecimal changePct;
        private BigDecimal latestPrice;
        private String industry;
        private List<String> concepts;
        private BigDecimal limitUpPrice;
        private BigDecimal limitDownPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeaderDTO {
        private String code;
        private String name;
        private BigDecimal changePct;
        private BigDecimal latestPrice;
        private String industry;
        private List<String> concepts;
        private BigDecimal limitUpPrice;
        private BigDecimal limitDownPrice;
        private Integer streak;
        private Integer previousStreak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DistributionRowDTO {
        private Integer streak;
        private Integer height;
        private Integer count;
        private List<StockDTO> stocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromotionLevelDTO {
        private Integer from;
        private Integer to;
        private Integer yesterdayCount;
        private Integer todayPromotedCount;
        private BigDecimal rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromotionDTO {
        private BigDecimal overallRate;
        private List<PromotionLevelDTO> levels;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrokenStockDTO {
        private String code;
        private String name;
        private BigDecimal changePct;
        private BigDecimal latestPrice;
        private String industry;
        private List<String> concepts;
        private BigDecimal limitUpPrice;
        private BigDecimal limitDownPrice;
        private Integer previousStreak;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrokenDTO {
        private Integer count;
        private Integer highStreakBrokenCount;
        private List<BrokenStockDTO> stocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SentimentDTO {
        private String level;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StreakDTO {
        private Integer maxHeight;
        private String label;
        private List<LeaderDTO> leaders;
        private List<DistributionRowDTO> distribution;
        private PromotionDTO promotion;
        private BrokenDTO broken;
        private SentimentDTO sentiment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BreakBoardDTO {
        private BigDecimal rate;
        private String label;
        private String status;
        private Integer touchedCount;
        private Integer brokenCount;
        private List<StockDTO> brokenStocks;
    }
}
