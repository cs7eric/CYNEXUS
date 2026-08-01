package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioTradeBO(
        Long id,
        String symbol,
        String securityName,
        TradeSide side,
        OffsetDateTime tradedAt,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalFee,
        TradeStatus status
) {
    public PortfolioTradeBO {
        if (id == null || symbol == null || symbol.isBlank() || side == null || tradedAt == null
                || quantity == null || quantity.signum() <= 0 || price == null || price.signum() <= 0
                || totalFee == null || totalFee.signum() < 0 || status == null) {
            throw new IllegalArgumentException("invalid portfolio trade");
        }
    }
}
