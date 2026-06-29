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
public class MarketPulseOverviewHistoryDTO {

    private Boolean ok;
    private String range;
    private String source;
    private Integer count;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private LocalDate date;
        private LocalDate tradeDate;
        private BigDecimal totalAmount;
        private BigDecimal totalVolume;
        private Integer risingCount;
        private Integer fallingCount;
        private Integer flatCount;
        private Integer limitUpCount;
        private Integer limitDownCount;
        private BigDecimal mainNetInflow;
        private BigDecimal superLargeNetInflow;
        private BigDecimal largeNetInflow;
        private BigDecimal mediumNetInflow;
        private BigDecimal smallNetInflow;
        private String source;
    }
}
