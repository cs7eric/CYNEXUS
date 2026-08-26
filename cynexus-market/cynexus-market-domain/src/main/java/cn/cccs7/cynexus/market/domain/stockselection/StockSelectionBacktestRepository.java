package cn.cccs7.cynexus.market.domain.stockselection;

import java.util.Optional;

public interface StockSelectionBacktestRepository {

    void save(StockSelectionBacktestBO.Run value);

    Optional<StockSelectionBacktestBO.ClaimedRun> claimNextQueued();

    void complete(StockSelectionBacktestBO.Run value);

    void fail(StockSelectionBacktestBO.ClaimedRun value, String warning);

    Optional<StockSelectionBacktestBO.StoredRun> findById(StockSelectionStrategyRunBO.Scope scope, long backtestId);

    default Optional<StockSelectionBacktestBO.StoredRun> findLatestCompletedByStrategy(StockSelectionStrategyRunBO.Scope scope,
                                                                                         String strategyCode) {
        return Optional.empty();
    }
}
