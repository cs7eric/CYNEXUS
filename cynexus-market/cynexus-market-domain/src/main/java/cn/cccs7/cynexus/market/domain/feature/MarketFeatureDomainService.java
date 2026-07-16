package cn.cccs7.cynexus.market.domain.feature;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;

public interface MarketFeatureDomainService {

    boolean isStrongTrend(StockDailyFeatureBO feature);
}
