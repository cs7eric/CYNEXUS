package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.application.service.MarketIndicatorQueryAppService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only materialized state endpoint; it deliberately has no provider fallback. */
@RestController
@RequestMapping("/api/market/trade-review")
public class TradeReviewMarketStateController {
    private final MarketIndicatorQueryAppService service;

    public TradeReviewMarketStateController(MarketIndicatorQueryAppService service) {
        this.service = service;
    }

    @GetMapping("/market-state")
    public Result<Map<String, Object>> marketState(@RequestParam(required = false) String tradeDate,
                                                    @RequestParam(required = false) String asOf,
                                                    @RequestParam(required = false) String mode,
                                                    @RequestParam(defaultValue = "union") String membership,
                                                    @RequestParam(required = false) String industryCode,
                                                    @RequestParam(defaultValue = "60") int windowDays) {
        return Result.success(service.tradeReviewMarketState(tradeDate, asOf, membership, industryCode, windowDays, mode));
    }

    @GetMapping("/market-state/status")
    public Result<Map<String, Object>> marketStateStatus(@RequestParam(required = false) String tradeDate) {
        return Result.success(service.tradeReviewMarketStateStatus(tradeDate));
    }

    @GetMapping("/market-state/dates")
    public Result<java.util.List<java.time.LocalDate>> materializedDates(@RequestParam(defaultValue = "120") int limit) {
        return Result.success(service.tradeReviewMaterializedDates(limit));
    }
}
