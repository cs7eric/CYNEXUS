package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;

public record PortfolioPositionBO(
        String symbol,
        PortfolioInstrumentType instrumentType,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal costBasis,
        BigDecimal realizedPnl,
        BigDecimal latestPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedReturnRate
) {
}
