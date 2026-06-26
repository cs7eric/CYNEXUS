package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import java.util.Optional;

public interface StockFeatureRepository {

    Optional<StockDailyFeatureBO> findDailyFeature(String symbol, String tradeDate);
}
