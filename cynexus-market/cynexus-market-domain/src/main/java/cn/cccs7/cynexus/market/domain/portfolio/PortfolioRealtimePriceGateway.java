package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Supplies the most recent executable market prices for portfolio valuation.
 * Implementations may use a live quote source and should return only prices
 * that were actually available; callers can fall back to daily bars.
 */
public interface PortfolioRealtimePriceGateway {
    Map<String, BigDecimal> latestPrices(List<PortfolioPositionBO> positions);
}
