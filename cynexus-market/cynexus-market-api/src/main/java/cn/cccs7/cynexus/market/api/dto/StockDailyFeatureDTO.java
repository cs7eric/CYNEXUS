package cn.cccs7.cynexus.market.api.dto;


import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDailyFeatureDTO {

    private String symbol;
    private String tradeDate;
    private BigDecimal closePrice;
    private BigDecimal changePercent;
    private BigDecimal turnoverRate;
    private BigDecimal volumeRatio;
    private BigDecimal mainNetInflow;
    private String trendLabel;
}
