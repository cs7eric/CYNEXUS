package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioTradeEquityPointBO(LocalDate date, BigDecimal cumulativePnl, BigDecimal drawdown) { }
