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
public class MarketPulseManualFundFlowDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradingDate;
    private OffsetDateTime savedAt;
    private String source;
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
