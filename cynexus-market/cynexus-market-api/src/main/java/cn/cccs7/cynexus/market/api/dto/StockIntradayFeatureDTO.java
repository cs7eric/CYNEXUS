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
public class StockIntradayFeatureDTO {

    private String symbol;
    private String tradeDate;
    private BigDecimal latestPrice;
    private BigDecimal amplitude;
    private BigDecimal volumeAcceleration;
    private BigDecimal mainNetInflow;
    private String intradayLabel;
}
