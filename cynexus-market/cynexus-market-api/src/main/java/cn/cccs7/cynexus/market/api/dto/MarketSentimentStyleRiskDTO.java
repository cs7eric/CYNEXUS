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
public class MarketSentimentStyleRiskDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private Integer windowDays;
    private IndexDTO hs300;
    private IndexDTO csi1000;
    private BigDecimal spread;
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
    public static class IndexDTO {
        private String name;
        private String code;
        private BigDecimal returnPct;
        private BigDecimal current;
        private BigDecimal baseClose;
        private LocalDate currentDate;
        private LocalDate baseDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private BigDecimal spread;
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
