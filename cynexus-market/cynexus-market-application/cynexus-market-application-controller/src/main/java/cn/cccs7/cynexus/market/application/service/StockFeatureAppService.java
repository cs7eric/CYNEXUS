package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class StockFeatureAppService {

    private final StockChartQueryDomainService domainService;

    public StockFeatureAppService(StockChartQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public StockDailyFeatureBO getDailyFeature(String symbol, String tradeDate) {
        return domainService.dailyFeature(symbol, tradeDate);
    }

    public StockIntradayFeatureBO getIntradayFeature(String symbol, String tradeDate) {
        return domainService.intradayFeature(symbol, tradeDate);
    }
}
