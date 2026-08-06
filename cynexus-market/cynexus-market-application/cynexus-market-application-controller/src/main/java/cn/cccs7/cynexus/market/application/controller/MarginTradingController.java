package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarginTradingHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustriesDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingIndustryHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingLatestDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStocksDTO;
import cn.cccs7.cynexus.market.api.dto.MarginTradingStockHistoryDTO;
import cn.cccs7.cynexus.market.application.service.MarginTradingAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/margin")
public class MarginTradingController {

    private final MarginTradingAppService marginTradingAppService;

    public MarginTradingController(MarginTradingAppService marginTradingAppService) {
        this.marginTradingAppService = marginTradingAppService;
    }

    @GetMapping("/latest")
    public Result<MarginTradingLatestDTO> latest(@RequestParam(required = false) String tradeDate,
                                                 @RequestParam(defaultValue = "20") int top) {
        return Result.success(marginTradingAppService.latest(tradeDate, top));
    }

    @GetMapping("/summary/history")
    public Result<MarginTradingHistoryDTO> summaryHistory(@RequestParam(required = false) String start,
                                                          @RequestParam(required = false) String end) {
        return Result.success(marginTradingAppService.summaryHistory(start, end));
    }

    @GetMapping("/stocks")
    public Result<MarginTradingStocksDTO> stocks(@RequestParam(required = false) String tradeDate,
                                                 @RequestParam(required = false) String exchange,
                                                 @RequestParam(required = false) String industryCode,
                                                 @RequestParam(defaultValue = "margin_balance") String sort,
                                                 @RequestParam(defaultValue = "desc") String sortDir,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize,
                                                 @RequestParam(defaultValue = "50") int limit) {
        return Result.success(marginTradingAppService.stocks(tradeDate, exchange, industryCode, sort, sortDir, page, pageSize, limit));
    }

    @GetMapping("/stocks/{stockCode}/history")
    public Result<MarginTradingStockHistoryDTO> stockHistory(@PathVariable String stockCode,
                                                             @RequestParam(defaultValue = "120") int limit) {
        return Result.success(marginTradingAppService.stockHistory(stockCode, limit));
    }

    @GetMapping("/industries")
    public Result<MarginTradingIndustriesDTO> industries(@RequestParam(required = false) String tradeDate,
                                                         @RequestParam(defaultValue = "margin_balance") String sort,
                                                         @RequestParam(defaultValue = "desc") String sortDir,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(marginTradingAppService.industries(tradeDate, sort, sortDir, page, pageSize));
    }

    @GetMapping("/industries/{industryCode}/history")
    public Result<MarginTradingIndustryHistoryDTO> industryHistory(@PathVariable String industryCode,
                                                                    @RequestParam(required = false) String tradeDate,
                                                                    @RequestParam(defaultValue = "60") int limit) {
        return Result.success(marginTradingAppService.industryHistory(industryCode, tradeDate, limit));
    }
}
