package cn.cccs7.cynexus.ai.infra.tool.market;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.feign.MarketFeignClient;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.stereotype.Component;

@Component
public class MarketDataTools implements MarketDataGateway {

    private final MarketFeignClient marketFeignClient;

    public MarketDataTools(MarketFeignClient marketFeignClient) {
        this.marketFeignClient = marketFeignClient;
    }

    @Override
    public MarketOverviewDTO getMarketOverview(String tradeDate) {
        return unwrap(marketFeignClient.getMarketOverview(tradeDate));
    }

    @Override
    public MarketPulseOverviewHistoryDTO getMarketOverviewHistory(String start, String end) {
        return unwrap(marketFeignClient.getMarketOverviewHistory(start, end));
    }

    @Override
    public SectorRankDTO getSectorRank(String tradeDate, Integer topN) {
        return unwrap(marketFeignClient.getSectorRank(tradeDate, topN == null ? 10 : topN));
    }

    @Override
    public StockDailyFeatureDTO getStockDailyFeature(String symbol, String tradeDate) {
        return unwrap(marketFeignClient.getStockDailyFeature(symbol, tradeDate));
    }

    @Override
    public StockIntradayFeatureDTO getStockIntradayFeature(String symbol, String tradeDate) {
        return unwrap(marketFeignClient.getStockIntradayFeature(symbol, tradeDate));
    }

    @Override
    public StockChartDTO.KlineResponseDTO getStockChartKlines(
            String targetType,
            String symbol,
            String period,
            String adjust) {
        return unwrap(marketFeignClient.getStockChartKlines(targetType, symbol, period, adjust));
    }

    private static <T> T unwrap(Result<T> result) {
        if (result == null || result.isFailed()) {
            return null;
        }
        return result.getData();
    }
}
