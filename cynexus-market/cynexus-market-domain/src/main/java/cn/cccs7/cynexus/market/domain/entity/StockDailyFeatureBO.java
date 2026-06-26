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
public class StockDailyFeatureBO {

    private Stock stock;
    private String tradeDate;
    private BigDecimal closePrice;
    private BigDecimal changePercent;
    private BigDecimal turnoverRate;
    private BigDecimal volumeRatio;
    private BigDecimal mainNetInflow;
    private String trendLabel;
}
