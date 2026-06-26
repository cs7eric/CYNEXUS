package cn.cccs7.cynexus.market.domain.entity;

import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;

public record StockIntradayFeatureBO(
        Stock stock,
        String tradeDate,
        BigDecimal latestPrice,
        BigDecimal amplitude,
        BigDecimal volumeAcceleration,
        BigDecimal mainNetInflow,
        String intradayLabel
) {
}
