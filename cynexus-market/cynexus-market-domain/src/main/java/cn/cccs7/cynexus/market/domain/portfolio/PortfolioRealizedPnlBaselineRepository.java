package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Reconciliation baselines used when an account starts tracking from an
 * existing broker ledger. The baseline offsets historical realized P&L while
 * keeping the confirmed trade rows available for audit and position tracking.
 */
public interface PortfolioRealizedPnlBaselineRepository {
    Map<String, BigDecimal> list(String principalKey, String tenantKey);
}
