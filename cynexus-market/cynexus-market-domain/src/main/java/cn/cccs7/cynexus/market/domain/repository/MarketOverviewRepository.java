package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import java.util.Optional;

public interface MarketOverviewRepository extends MarketCrudRepository<MarketOverviewBO> {

    Optional<MarketOverviewBO> findByTradeDate(String tradeDate);
}
