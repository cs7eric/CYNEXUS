package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Derived, point-in-time assessment of whether a governed strategy is usable today. */
public final class StockSelectionStrategyHealthBO {

    private StockSelectionStrategyHealthBO() {
    }

    public record Assessment(String strategyCode,
                             int strategyVersionNo,
                             LocalDate requestedTradeDate,
                             LocalDate actualTradeDate,
                             String status,
                             String reason,
                             String recommendedAction,
                             int universeCount,
                             int candidateCount,
                             BigDecimal signalDensityPct,
                             long freshnessLagDays,
                             RecentPerformance recentPerformance,
                             String source,
                             List<Check> checks,
                             List<String> warnings) {
    }

    public record Check(String code,
                        String label,
                        String status,
                        String actualValue,
                             String threshold,
                             String detail) {
    }

    public record RecentPerformance(int evaluatedTradeCount,
                                   BigDecimal winRatePct,
                                   BigDecimal averageExcessReturnPct,
                                   LocalDate asOfDate) {
    }
}
