package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;

/**
 * A non-fatal ledger quality issue discovered while calculating the portfolio.
 *
 * <p>Imports are often partial snapshots of a broker's history. A sell that
 * predates the imported buy history must not make the complete overview
 * unavailable; it is kept as a warning and excluded from unreliable P&amp;L
 * matching.</p>
 */
public record PortfolioLedgerWarningBO(
        String code,
        String symbol,
        BigDecimal unmatchedQuantity,
        String message
) {
}
