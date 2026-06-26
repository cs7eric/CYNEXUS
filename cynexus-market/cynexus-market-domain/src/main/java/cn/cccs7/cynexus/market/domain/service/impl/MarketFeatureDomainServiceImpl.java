package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.service.MarketFeatureDomainService;

public class MarketFeatureDomainServiceImpl implements MarketFeatureDomainService {

    @Override
    public boolean isStrongTrend(StockDailyFeatureBO feature) {
        return feature.changePercent().doubleValue() > 3.0 && feature.volumeRatio().doubleValue() > 1.5;
    }
}
