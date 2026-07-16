package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/market")
public class AiMarketProbeController {

    private final MarketDataGateway marketDataGateway;

    public AiMarketProbeController(MarketDataGateway marketDataGateway) {
        this.marketDataGateway = marketDataGateway;
    }

    @GetMapping("/overview")
    public Result<MarketOverviewDTO> getMarketOverview(@RequestParam("tradeDate") String tradeDate) {
        return Result.success(marketDataGateway.getMarketOverview(tradeDate));
    }

    @GetMapping("/sectors/rank")
    public Result<SectorRankDTO> getSectorRank(@RequestParam("tradeDate") String tradeDate,
                                               @RequestParam(name = "topN", defaultValue = "10") Integer topN) {
        return Result.success(marketDataGateway.getSectorRank(tradeDate, topN));
    }

    @GetMapping("/stocks/{symbol}/daily-feature")
    public Result<StockDailyFeatureDTO> getStockDailyFeature(@PathVariable("symbol") String symbol,
                                                             @RequestParam("tradeDate") String tradeDate) {
        return Result.success(marketDataGateway.getStockDailyFeature(symbol, tradeDate));
    }

    @GetMapping("/stocks/{symbol}/intraday-feature")
    public Result<StockIntradayFeatureDTO> getStockIntradayFeature(@PathVariable("symbol") String symbol,
                                                                   @RequestParam("tradeDate") String tradeDate) {
        return Result.success(marketDataGateway.getStockIntradayFeature(symbol, tradeDate));
    }
}
