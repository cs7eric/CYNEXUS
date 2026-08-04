package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioAccountBO(BigDecimal totalAssets, BigDecimal legacyAvailableCash, OffsetDateTime updatedAt) {
    public PortfolioAccountBO {
        legacyAvailableCash = legacyAvailableCash == null ? BigDecimal.ZERO : legacyAvailableCash;
    }
}
