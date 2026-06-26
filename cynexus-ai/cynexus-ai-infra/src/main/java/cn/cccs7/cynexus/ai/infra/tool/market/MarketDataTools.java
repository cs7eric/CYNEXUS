package cn.cccs7.cynexus.ai.infra.tool.market;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.feign.MarketFeignClient;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.stereotype.Component;

@Component
public class MarketDataTools {

    private final MarketFeignClient marketFeignClient;

    public MarketDataTools(MarketFeignClient marketFeignClient) {
        this.marketFeignClient = marketFeignClient;
    }

    public MarketOverviewDTO getMarketOverview(String tradeDate) {
        return unwrap(marketFeignClient.getMarketOverview(tradeDate));
    }

    public SectorRankDTO getSectorRank(String tradeDate, Integer topN) {
        return unwrap(marketFeignClient.getSectorRank(tradeDate, topN == null ? 10 : topN));
    }

    public StockDailyFeatureDTO getStockDailyFeature(String symbol, String tradeDate) {
        return unwrap(marketFeignClient.getStockDailyFeature(symbol, tradeDate));
    }

    public StockIntradayFeatureDTO getStockIntradayFeature(String symbol, String tradeDate) {
        return unwrap(marketFeignClient.getStockIntradayFeature(symbol, tradeDate));
    }

    private static <T> T unwrap(Result<T> result) {
        if (result == null || result.isFailed()) {
            return null;
        }
        return result.getData();
    }
}
