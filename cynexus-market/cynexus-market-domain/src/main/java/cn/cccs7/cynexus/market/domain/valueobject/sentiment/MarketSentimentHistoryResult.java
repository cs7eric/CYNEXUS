package cn.cccs7.cynexus.market.domain.valueobject.sentiment;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain result for a sentiment history query. The application layer maps the
 * rows to the public API DTOs.
 */
public record MarketSentimentHistoryResult<T>(
        LocalDate start,
        LocalDate end,
        List<Item<T>> items,
        String source) {

    public MarketSentimentHistoryResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item<T>(T value, java.math.BigDecimal resolvedScore) {
    }
}
