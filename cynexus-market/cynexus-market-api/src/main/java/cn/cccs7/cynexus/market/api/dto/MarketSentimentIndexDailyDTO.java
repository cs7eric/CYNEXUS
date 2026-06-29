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
public class MarketSentimentIndexDailyDTO {

    private Boolean ok;
    private String code;
    private String name;
    private LocalDate start;
    private LocalDate end;
    private Integer count;
    private List<ItemDTO> items;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private LocalDate tradeDate;
        private BigDecimal close;
        private BigDecimal amount;
    }
}
