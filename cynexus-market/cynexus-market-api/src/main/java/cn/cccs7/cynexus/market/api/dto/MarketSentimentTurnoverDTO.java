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
public class MarketSentimentTurnoverDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private BigDecimal totalAmount;
    private BigDecimal avg20dAmount;
    private BigDecimal ratio;
    private BigDecimal score;
    private BigDecimal rawValue;
    private Integer elapsedMs;
    private String source;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private BigDecimal totalAmount;
        private BigDecimal avg20dAmount;
        private BigDecimal ratio;
        private BigDecimal score;
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
