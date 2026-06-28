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
public class MarketPulseIndexReturnsDTO {

    private Boolean ok;
    private Integer days;
    private List<ItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private String name;
        private String code;
        private String fullCode;
        private BigDecimal current;
        private LocalDate currentDate;
        private BigDecimal baseClose;
        private LocalDate baseDate;
        private BigDecimal returnPct;
        private List<BigDecimal> daily;
        private Boolean available;
    }
}
