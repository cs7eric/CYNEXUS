package cn.cccs7.cynexus.market.domain.valueobject.sentiment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of a sentiment metric lookup after the domain resolves its serving date.
 */
public record MarketSentimentQueryResult<T>(
        LocalDate requestedDate,
        LocalDate effectiveDate,
        T value,
        BigDecimal resolvedScore) {

    public boolean present() {
        return value != null;
    }
}
