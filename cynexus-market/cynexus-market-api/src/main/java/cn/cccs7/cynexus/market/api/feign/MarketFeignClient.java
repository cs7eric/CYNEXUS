package cn.cccs7.cynexus.market.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cynexus-market", path = "/api/market")
public interface MarketFeignClient {

    @GetMapping("/overview")
    Result<MarketOverviewDTO> getMarketOverview(@RequestParam("tradeDate") String tradeDate);

    @GetMapping("/sectors/rank")
    Result<SectorRankDTO> getSectorRank(@RequestParam("tradeDate") String tradeDate,
                                        @RequestParam("topN") Integer topN);

    @GetMapping("/stocks/{symbol}/daily-feature")
    Result<StockDailyFeatureDTO> getStockDailyFeature(@PathVariable("symbol") String symbol,
                                                      @RequestParam("tradeDate") String tradeDate);

    @GetMapping("/stocks/{symbol}/intraday-feature")
    Result<StockIntradayFeatureDTO> getStockIntradayFeature(@PathVariable("symbol") String symbol,
                                                            @RequestParam("tradeDate") String tradeDate);
}
