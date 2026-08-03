package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioPerformancePointBO(
        LocalDate tradeDate,
        BigDecimal marketValue,
        BigDecimal totalAssets,
        BigDecimal costBasis,
        BigDecimal totalPnl,
        BigDecimal periodPnl,
        BigDecimal returnRate
) {
}
