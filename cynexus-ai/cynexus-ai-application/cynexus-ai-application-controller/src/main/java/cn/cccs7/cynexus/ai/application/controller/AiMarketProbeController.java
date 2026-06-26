package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.infra.tool.market.MarketDataTools;
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

    private final MarketDataTools marketDataTools;

    public AiMarketProbeController(MarketDataTools marketDataTools) {
        this.marketDataTools = marketDataTools;
    }

    @GetMapping("/overview")
    public Result<MarketOverviewDTO> getMarketOverview(@RequestParam String tradeDate) {
        return Result.success(marketDataTools.getMarketOverview(tradeDate));
    }

    @GetMapping("/sectors/rank")
    public Result<SectorRankDTO> getSectorRank(@RequestParam String tradeDate,
                                               @RequestParam(defaultValue = "10") Integer topN) {
        return Result.success(marketDataTools.getSectorRank(tradeDate, topN));
    }

    @GetMapping("/stocks/{symbol}/daily-feature")
    public Result<StockDailyFeatureDTO> getStockDailyFeature(@PathVariable String symbol,
                                                             @RequestParam String tradeDate) {
        return Result.success(marketDataTools.getStockDailyFeature(symbol, tradeDate));
    }

    @GetMapping("/stocks/{symbol}/intraday-feature")
    public Result<StockIntradayFeatureDTO> getStockIntradayFeature(@PathVariable String symbol,
                                                                   @RequestParam String tradeDate) {
        return Result.success(marketDataTools.getStockIntradayFeature(symbol, tradeDate));
    }
}
