package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioLedgerResultBO(
        List<PortfolioPositionBO> positions,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        BigDecimal totalPnl
) {
    public PortfolioLedgerResultBO {
        positions = positions == null ? List.of() : List.copyOf(positions);
        realizedPnl = realizedPnl == null ? BigDecimal.ZERO : realizedPnl;
        unrealizedPnl = unrealizedPnl == null ? BigDecimal.ZERO : unrealizedPnl;
        totalPnl = totalPnl == null ? realizedPnl.add(unrealizedPnl) : totalPnl;
    }
}
