package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        List<PortfolioPerformancePointBO> points
) {
    public PortfolioPerformanceResultBO {
        points = points == null ? List.of() : List.copyOf(points);
    }
}
