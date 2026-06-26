package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;

public record StockIntradayFeatureDTO(
        String symbol,
        String tradeDate,
        BigDecimal latestPrice,
        BigDecimal amplitude,
        BigDecimal volumeAcceleration,
        BigDecimal mainNetInflow,
        String intradayLabel
) {
}
