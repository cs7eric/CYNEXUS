package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioPricePointBO(LocalDate tradeDate, BigDecimal close) {
    public PortfolioPricePointBO {
        if (tradeDate == null || close == null || close.signum() <= 0) {
            throw new IllegalArgumentException("invalid portfolio price point");
        }
    }
}
