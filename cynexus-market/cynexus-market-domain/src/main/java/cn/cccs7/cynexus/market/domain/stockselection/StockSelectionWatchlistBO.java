package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class StockSelectionWatchlistBO {
    private StockSelectionWatchlistBO() { }
    public record Candidate(long id, String stockCode, String stockName, String market, LocalDate tradeDate, BigDecimal entryPrice) { }
    public record Watchlist(long id, StockSelectionStrategyRunBO.Scope scope, String name, long version) { }
    public record Item(long id, StockSelectionStrategyRunBO.Scope scope, String stockCode, String stockName,
                       Long sourceCandidateId, String status, String attentionReason, String invalidationCondition,
                       LocalDate nextReviewDate, long version, OffsetDateTime updatedAt, long watchlistId, String watchlistName,
                       String market, LocalDate entryTradeDate, BigDecimal entryPrice) {
        public Item(long id, StockSelectionStrategyRunBO.Scope scope, String stockCode, String stockName, Long sourceCandidateId,
                    String status, String attentionReason, String invalidationCondition, LocalDate nextReviewDate, long version,
                    OffsetDateTime updatedAt) { this(id, scope, stockCode, stockName, sourceCandidateId, status, attentionReason,
                    invalidationCondition, nextReviewDate, version, updatedAt, 0, "My watchlist", null, null, null); }
    }
    public record Review(long id, long itemId, String decision, String note, OffsetDateTime reviewedAt) { }
    public record Detail(Item item, List<Review> reviews) { }
}
