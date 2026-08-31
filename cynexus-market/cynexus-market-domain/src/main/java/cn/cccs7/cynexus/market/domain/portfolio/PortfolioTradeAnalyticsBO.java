package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioTradeAnalyticsBO(
        LocalDate from,
        LocalDate to,
        int completedCount,
        int openCount,
        int winCount,
        int lossCount,
        int breakevenCount,
        BigDecimal winRate,
        BigDecimal averageWin,
        BigDecimal averageLoss,
        BigDecimal payoffRatio,
        BigDecimal profitFactor,
        BigDecimal expectancy,
        BigDecimal netPnl,
        BigDecimal maxWin,
        BigDecimal maxLoss,
        int maxConsecutiveWins,
        int maxConsecutiveLosses,
        BigDecimal averageHoldingDays,
        BigDecimal medianHoldingDays,
        BigDecimal feeRatio,
        BigDecimal maxDrawdown,
        List<PortfolioTradeRoundBO> rounds,
        List<PortfolioTradeEquityPointBO> equityCurve,
        List<PortfolioTradeAnalyticsWarningBO> dataQualityWarnings) {
}
