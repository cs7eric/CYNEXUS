package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import cn.cccs7.cynexus.market.application.assembler.StockDailyFeatureAssembler;
import cn.cccs7.cynexus.market.application.assembler.StockIntradayFeatureAssembler;
import cn.cccs7.cynexus.market.application.service.StockFeatureAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/stocks")
public class StockFeatureController {

    private final StockFeatureAppService stockFeatureAppService;
    private final StockDailyFeatureAssembler stockDailyFeatureAssembler;
    private final StockIntradayFeatureAssembler stockIntradayFeatureAssembler;

    public StockFeatureController(StockFeatureAppService stockFeatureAppService,
                                  StockDailyFeatureAssembler stockDailyFeatureAssembler,
                                  StockIntradayFeatureAssembler stockIntradayFeatureAssembler) {
        this.stockFeatureAppService = stockFeatureAppService;
        this.stockDailyFeatureAssembler = stockDailyFeatureAssembler;
        this.stockIntradayFeatureAssembler = stockIntradayFeatureAssembler;
    }

    @GetMapping("/{symbol}/daily-feature")
    public Result<StockDailyFeatureDTO> getStockDailyFeature(@PathVariable String symbol, @RequestParam String tradeDate) {
        return Result.success(stockDailyFeatureAssembler.toTarget(stockFeatureAppService.getDailyFeature(symbol, tradeDate)));
    }

    @GetMapping("/{symbol}/intraday-feature")
    public Result<StockIntradayFeatureDTO> getStockIntradayFeature(@PathVariable String symbol, @RequestParam String tradeDate) {
        return Result.success(stockIntradayFeatureAssembler.toTarget(stockFeatureAppService.getIntradayFeature(symbol, tradeDate)));
    }
}
