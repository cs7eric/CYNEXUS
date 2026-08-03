package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Supplies the most recent executable market prices for portfolio valuation.
 * Implementations may use a live quote source and should return only prices
 * that were actually available; callers can fall back to daily bars.
 */
public interface PortfolioRealtimePriceGateway {
    Map<String, BigDecimal> latestPrices(List<PortfolioPositionBO> positions);

    /** Returns the trading date associated with each latest quote when available. */
    default Map<String, LocalDate> latestPriceDates(List<PortfolioPositionBO> positions) {
        return Map.of();
    }

    /** Returns the previous trading close when the quote provider supplies it. */
    default Map<String, BigDecimal> previousClosePrices(List<PortfolioPositionBO> positions) {
        return Map.of();
    }
}
