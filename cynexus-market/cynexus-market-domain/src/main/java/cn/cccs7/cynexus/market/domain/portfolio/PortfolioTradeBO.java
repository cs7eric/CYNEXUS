package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioTradeBO(
        Long id,
        String symbol,
        String securityName,
        PortfolioInstrumentType instrumentType,
        TradeSide side,
        OffsetDateTime tradedAt,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalFee,
        TradeStatus status,
        String strategy,
        String tags,
        String industryCode,
        String industryName
) {
    public PortfolioTradeBO(Long id, String symbol, String securityName, PortfolioInstrumentType instrumentType,
                            TradeSide side, OffsetDateTime tradedAt, BigDecimal quantity, BigDecimal price,
                            BigDecimal totalFee, TradeStatus status) {
        this(id, symbol, securityName, instrumentType, side, tradedAt, quantity, price, totalFee, status, null, "[]", null, null);
    }
    public PortfolioTradeBO(Long id, String symbol, String securityName, PortfolioInstrumentType instrumentType,
                            TradeSide side, OffsetDateTime tradedAt, BigDecimal quantity, BigDecimal price,
                            BigDecimal totalFee, TradeStatus status, String strategy, String tags) {
        this(id, symbol, securityName, instrumentType, side, tradedAt, quantity, price, totalFee, status, strategy, tags, null, null);
    }
    public PortfolioTradeBO {
        if (id == null || symbol == null || symbol.isBlank() || instrumentType == null || side == null || tradedAt == null
                || quantity == null || quantity.signum() <= 0 || price == null || price.signum() <= 0
                || totalFee == null || totalFee.signum() < 0 || status == null) {
            throw new IllegalArgumentException("invalid portfolio trade");
        }
        // portfolio_trade.tags is a NOT NULL JSONB column. Keep the domain
        // default aligned with persistence when an import has no tags.
        if (tags == null || tags.isBlank()) tags = "[]";
    }
}
