package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioAccountBO(BigDecimal availableCash, OffsetDateTime updatedAt) {
    public PortfolioAccountBO {
        availableCash = availableCash == null ? BigDecimal.ZERO : availableCash;
    }
}
