package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;

public interface MarketFeatureDomainService {

    boolean isStrongTrend(StockDailyFeatureBO feature);
}
