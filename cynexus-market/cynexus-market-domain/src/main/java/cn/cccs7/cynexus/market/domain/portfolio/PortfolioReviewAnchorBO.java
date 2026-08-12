package cn.cccs7.cynexus.market.domain.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

public record PortfolioReviewAnchorBO(Long id, String principalKey, String tenantKey, String symbol,
                                      String instrumentType, Long tradeId, Long reviewId, String anchorType,
                                      String side, OffsetDateTime occurredAt, BigDecimal price, String note,
                                      String tags) {
    public PortfolioReviewAnchorBO {
        symbol = text(symbol).toUpperCase(Locale.ROOT);
        instrumentType = text(instrumentType).toUpperCase(Locale.ROOT);
        anchorType = text(anchorType).toUpperCase(Locale.ROOT);
        side = text(side).toUpperCase(Locale.ROOT);
        if (symbol.isBlank() || !("STOCK".equals(instrumentType) || "ETF".equals(instrumentType))
                || !("TRADE".equals(anchorType) || "OBSERVATION".equals(anchorType))
                || occurredAt == null || price == null || price.signum() < 0
                || ("TRADE".equals(anchorType) && tradeId == null)
                || !("BUY".equals(side) || "SELL".equals(side) || "OBSERVATION".equals(side) || "EVENT".equals(side))) {
            throw new IllegalArgumentException("invalid portfolio review anchor");
        }
        note = note == null ? null : note.trim();
        if (note != null && note.length() > 2000) throw new IllegalArgumentException("portfolio review anchor note is too long");
        tags = PortfolioReviewContentPolicy.normalizeTags(tags);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
