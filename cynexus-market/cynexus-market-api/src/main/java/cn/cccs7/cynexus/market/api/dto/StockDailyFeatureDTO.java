package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;

public record StockDailyFeatureDTO(
        String symbol,
        String tradeDate,
        BigDecimal closePrice,
        BigDecimal changePercent,
        BigDecimal turnoverRate,
        BigDecimal volumeRatio,
        BigDecimal mainNetInflow,
        String trendLabel
) {
}
