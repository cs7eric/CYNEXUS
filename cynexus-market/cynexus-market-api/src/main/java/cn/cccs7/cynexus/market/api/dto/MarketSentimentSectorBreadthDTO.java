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
public class MarketSentimentSectorBreadthDTO {

    private Boolean ok;
    private LocalDate tradeDate;
    private Integer advancing;
    private Integer declining;
    private Integer flat;
    private Integer total;
    private BigDecimal advancePct;
    private BigDecimal score;
    private String source;
    private Integer elapsedMs;
    private Boolean fromCache;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItemDTO {
        private LocalDate tradeDate;
        private Integer advancing;
        private Integer declining;
        private Integer flat;
        private Integer total;
        private BigDecimal advancePct;
        private BigDecimal score;
        private String source;
        private Integer elapsedMs;
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
        private Integer days;
        private Integer count;
        private List<HistoryItemDTO> items;
        private String error;
    }
}
