package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPulseOverviewDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradingDate;
    private OffsetDateTime fetchedAt;
    private String source;
    private Boolean isTradeTime;
    private BigDecimal totalAmount;
    private BigDecimal totalVolume;
    private Integer stockCount;
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
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetInflowRatio;
    private BigDecimal largeNetInflowRatio;
    private BigDecimal mediumNetInflowRatio;
    private BigDecimal smallNetInflowRatio;
    private MarketPulsePrevDayFlowDTO prevDayFlow;
    private LocalDate prevDayTradingDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketPulsePrevDayFlowDTO {
        private BigDecimal mainNetInflow;
        private BigDecimal superLargeNetInflow;
        private BigDecimal largeNetInflow;
        private BigDecimal mediumNetInflow;
        private BigDecimal smallNetInflow;
        private BigDecimal totalAmount;
    }
}
