package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.policy.market.MarketProbeQueryPolicy;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.stereotype.Service;

/** Application query boundary for the AI-facing market probe endpoints. */
@Service
public class MarketProbeQueryAppService {

    private final MarketDataGateway gateway;
    private final MarketProbeQueryPolicy policy = new MarketProbeQueryPolicy();

    public MarketProbeQueryAppService(MarketDataGateway gateway) {
        this.gateway = gateway;
    }

    public MarketOverviewDTO marketOverview(String tradeDate) {
        return gateway.getMarketOverview(policy.requireTradeDate(tradeDate));
    }

    public SectorRankDTO sectorRank(String tradeDate, Integer topN) {
        return gateway.getSectorRank(policy.requireTradeDate(tradeDate), policy.normalizeTopN(topN));
    }

    public StockDailyFeatureDTO stockDailyFeature(String symbol, String tradeDate) {
        return gateway.getStockDailyFeature(policy.requireSymbol(symbol), policy.requireTradeDate(tradeDate));
    }

    public StockIntradayFeatureDTO stockIntradayFeature(String symbol, String tradeDate) {
        return gateway.getStockIntradayFeature(policy.requireSymbol(symbol), policy.requireTradeDate(tradeDate));
    }
}
