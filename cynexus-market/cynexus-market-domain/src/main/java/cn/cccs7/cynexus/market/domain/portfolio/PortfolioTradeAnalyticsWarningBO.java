package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;

public record PortfolioTradeAnalyticsWarningBO(String code, String symbol, BigDecimal quantity, String message) {
}
