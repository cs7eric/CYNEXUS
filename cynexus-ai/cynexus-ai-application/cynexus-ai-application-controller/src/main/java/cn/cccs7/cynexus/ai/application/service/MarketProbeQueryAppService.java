package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.stereotype.Service;

/** Application query boundary for the AI-facing market probe endpoints. */
@Service
public class MarketProbeQueryAppService {

    private final MarketDataGateway gateway;

    public MarketProbeQueryAppService(MarketDataGateway gateway) {
        this.gateway = gateway;
    }

    public MarketOverviewDTO marketOverview(String tradeDate) {
        return gateway.getMarketOverview(requireTradeDate(tradeDate));
    }

    public SectorRankDTO sectorRank(String tradeDate, Integer topN) {
        int normalizedTopN = topN == null ? 10 : Math.max(1, Math.min(topN, 100));
        return gateway.getSectorRank(requireTradeDate(tradeDate), normalizedTopN);
    }

    public StockDailyFeatureDTO stockDailyFeature(String symbol, String tradeDate) {
        return gateway.getStockDailyFeature(requireSymbol(symbol), requireTradeDate(tradeDate));
    }

    public StockIntradayFeatureDTO stockIntradayFeature(String symbol, String tradeDate) {
        return gateway.getStockIntradayFeature(requireSymbol(symbol), requireTradeDate(tradeDate));
    }

    private String requireTradeDate(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("tradeDate is required");
        return value.trim();
    }

    private String requireSymbol(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("symbol is required");
        return value.trim();
    }
}
