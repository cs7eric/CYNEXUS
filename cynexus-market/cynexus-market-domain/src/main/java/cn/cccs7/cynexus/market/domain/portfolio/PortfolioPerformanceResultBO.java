package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PortfolioPerformanceResultBO(
        PortfolioPerformanceGranularity granularity,
        LocalDate from,
        LocalDate to,
        BigDecimal periodPnl,
        BigDecimal periodReturnRate,
        BigDecimal startAssets,
        BigDecimal endAssets,
        BigDecimal bestPeriodPnl,
        BigDecimal worstPeriodPnl,
        int tradingDays,
        List<PortfolioPerformancePointBO> points,
        Map<String, BigDecimal> periodPnlBySymbol,
        Map<String, BigDecimal> lastDayPnlBySymbol
) {
    public PortfolioPerformanceResultBO {
        points = points == null ? List.of() : List.copyOf(points);
        periodPnlBySymbol = periodPnlBySymbol == null ? Map.of() : Map.copyOf(periodPnlBySymbol);
        lastDayPnlBySymbol = lastDayPnlBySymbol == null ? Map.of() : Map.copyOf(lastDayPnlBySymbol);
    }

    public PortfolioPerformanceResultBO(
            PortfolioPerformanceGranularity granularity,
            LocalDate from,
            LocalDate to,
            BigDecimal periodPnl,
            BigDecimal periodReturnRate,
            BigDecimal startAssets,
            BigDecimal endAssets,
            BigDecimal bestPeriodPnl,
            BigDecimal worstPeriodPnl,
            int tradingDays,
            List<PortfolioPerformancePointBO> points) {
        this(granularity, from, to, periodPnl, periodReturnRate, startAssets, endAssets,
                bestPeriodPnl, worstPeriodPnl, tradingDays, points, Map.of(), Map.of());
    }
}
