package cn.cccs7.cynexus.ai.api.gateway;

import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;

public interface MarketDataGateway {

    MarketOverviewDTO getMarketOverview(String tradeDate);

    MarketPulseOverviewHistoryDTO getMarketOverviewHistory(String start, String end);

    SectorRankDTO getSectorRank(String tradeDate, Integer topN);

    StockDailyFeatureDTO getStockDailyFeature(String symbol, String tradeDate);

    StockIntradayFeatureDTO getStockIntradayFeature(String symbol, String tradeDate);

    StockChartDTO.KlineResponseDTO getStockChartKlines(String targetType, String symbol, String period, String adjust);
}
