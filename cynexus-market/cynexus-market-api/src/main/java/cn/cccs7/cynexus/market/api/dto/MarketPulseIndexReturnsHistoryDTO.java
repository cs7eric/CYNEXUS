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
public class MarketPulseIndexReturnsHistoryDTO {

    private Boolean ok;
    private Integer window;
    private LocalDate start;
    private LocalDate end;
    private Integer count;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private LocalDate tradeDate;
        private String code;
        private String name;
        private BigDecimal current;
        private LocalDate currentDate;
        private BigDecimal baseClose;
        private LocalDate baseDate;
        private BigDecimal returnPct;
    }
}
