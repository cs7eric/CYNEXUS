package cn.cccs7.cynexus.market.domain.feature.impl;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.feature.MarketFeatureDomainService;

public class MarketFeatureDomainServiceImpl implements MarketFeatureDomainService {

    @Override
    public boolean isStrongTrend(StockDailyFeatureBO feature) {
        return feature != null
                && feature.getChangePercent() != null
                && feature.getVolumeRatio() != null
                && feature.getChangePercent().doubleValue() > 3.0
                && feature.getVolumeRatio().doubleValue() > 1.5;
    }
}
