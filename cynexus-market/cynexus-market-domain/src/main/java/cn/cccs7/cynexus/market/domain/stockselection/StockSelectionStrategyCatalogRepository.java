package cn.cccs7.cynexus.market.domain.stockselection;

import java.util.List;

public interface StockSelectionStrategyCatalogRepository {

    List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies();

    default List<StockSelectionStrategyCatalogBO.Strategy> findActiveStrategies(StockSelectionStrategyRunBO.Scope scope) {
        return findActiveStrategies();
    }

    default boolean createUserStrategy(StockSelectionStrategyRunBO.Scope scope,
                                       StockSelectionStrategyCatalogBO.Strategy strategy) {
        throw new UnsupportedOperationException("user strategy writes are unavailable");
    }

    default boolean appendUserStrategyVersion(StockSelectionStrategyRunBO.Scope scope,
                                              StockSelectionStrategyCatalogBO.Strategy strategy,
                                              long expectedDefinitionVersion) {
        throw new UnsupportedOperationException("user strategy writes are unavailable");
    }
}
