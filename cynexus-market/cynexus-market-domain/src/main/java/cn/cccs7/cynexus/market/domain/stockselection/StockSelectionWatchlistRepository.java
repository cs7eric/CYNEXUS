package cn.cccs7.cynexus.market.domain.stockselection;

import java.util.List;
import java.util.Optional;

public interface StockSelectionWatchlistRepository {
    Optional<StockSelectionWatchlistBO.Candidate> findCandidate(StockSelectionStrategyRunBO.Scope scope, long candidateId);
    Optional<StockSelectionWatchlistBO.Item> findActive(StockSelectionStrategyRunBO.Scope scope, long watchlistId, String stockCode);
    Optional<StockSelectionWatchlistBO.Watchlist> findActiveWatchlist(StockSelectionStrategyRunBO.Scope scope, String name);
    void saveWatchlist(StockSelectionWatchlistBO.Watchlist watchlist);
    void save(StockSelectionWatchlistBO.Item item);
    List<StockSelectionWatchlistBO.Detail> list(StockSelectionStrategyRunBO.Scope scope);
    boolean update(StockSelectionWatchlistBO.Item item, long expectedVersion);
    void saveReview(StockSelectionWatchlistBO.Review review, StockSelectionStrategyRunBO.Scope scope);
}
