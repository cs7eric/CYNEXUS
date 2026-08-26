package cn.cccs7.cynexus.market.domain.stockselection;

import java.util.Optional;

public interface StockSelectionStrategyRunRepository {

    void save(StockSelectionStrategyRunBO.Run run);

    Optional<StockSelectionStrategyRunBO.HealthInput> findLatestHealthInput(StockSelectionStrategyRunBO.Scope scope,
                                                                              String strategyCode);
}
