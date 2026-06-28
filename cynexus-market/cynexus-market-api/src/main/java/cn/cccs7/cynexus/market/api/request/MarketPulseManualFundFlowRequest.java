package cn.cccs7.cynexus.market.api.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPulseManualFundFlowRequest {

    private LocalDate tradingDate;
    private BigDecimal mainNetInflow;
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetInflow;
    private BigDecimal superLargeNetInflowRatio;
    private BigDecimal largeNetInflow;
    private BigDecimal largeNetInflowRatio;
    private BigDecimal mediumNetInflow;
    private BigDecimal mediumNetInflowRatio;
    private BigDecimal smallNetInflow;
    private BigDecimal smallNetInflowRatio;
}
