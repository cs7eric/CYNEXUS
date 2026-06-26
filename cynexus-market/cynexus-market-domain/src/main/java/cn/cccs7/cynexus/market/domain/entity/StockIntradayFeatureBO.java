package cn.cccs7.cynexus.market.domain.entity;

import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockIntradayFeatureBO {

    private Stock stock;
    private String tradeDate;
    private BigDecimal latestPrice;
    private BigDecimal amplitude;
    private BigDecimal volumeAcceleration;
    private BigDecimal mainNetInflow;
    private String intradayLabel;
}
