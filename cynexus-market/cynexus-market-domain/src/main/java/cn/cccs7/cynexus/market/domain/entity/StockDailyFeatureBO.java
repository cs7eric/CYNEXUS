package cn.cccs7.cynexus.market.domain.entity;

import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;

public record StockDailyFeatureBO(
        Stock stock,
        String tradeDate,
        BigDecimal closePrice,
        BigDecimal changePercent,
        BigDecimal turnoverRate,
        BigDecimal volumeRatio,
        BigDecimal mainNetInflow,
        String trendLabel
) {
}
