package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioTradeAnalyticsDTO {
    LocalDate from;
    LocalDate to;
    int completedCount;
    int openCount;
    int winCount;
    int lossCount;
    int breakevenCount;
    BigDecimal winRate;
    BigDecimal averageWin;
    BigDecimal averageLoss;
    BigDecimal payoffRatio;
    BigDecimal profitFactor;
    BigDecimal expectancy;
    BigDecimal netPnl;
    BigDecimal maxWin;
    BigDecimal maxLoss;
    int maxConsecutiveWins;
    int maxConsecutiveLosses;
    BigDecimal averageHoldingDays;
    BigDecimal medianHoldingDays;
    BigDecimal feeRatio;
    BigDecimal maxDrawdown;
    BigDecimal timeWeightedReturn;
    BigDecimal moneyWeightedReturn;
    BigDecimal accountMaxDrawdown;
    BigDecimal accountMaxDrawdownRate;
    List<PortfolioAccountSnapshotDTO> accountSnapshots;
    List<PortfolioCashFlowDTO> cashFlows;
    List<PortfolioTradeRoundDTO> rounds;
    List<PortfolioTradeEquityPointDTO> equityCurve;
    List<PortfolioTradeAnalyticsWarningDTO> dataQualityWarnings;
}
