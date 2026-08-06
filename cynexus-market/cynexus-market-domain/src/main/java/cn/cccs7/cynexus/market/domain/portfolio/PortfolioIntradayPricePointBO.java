package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortfolioIntradayPricePointBO(LocalDateTime tradedAt, BigDecimal price) {
    public PortfolioIntradayPricePointBO {
        if (tradedAt == null || price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("invalid intraday price point");
        }
    }
}
