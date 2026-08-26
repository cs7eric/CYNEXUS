package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockSelectionWatchlistDTO;
import cn.cccs7.cynexus.market.application.service.StockSelectionWatchlistAppService;
import cn.cccs7.cynexus.market.application.service.stockselection.StockSelectionIdentityResolver;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener/watchlist")
public class StockSelectionWatchlistController {

    private final StockSelectionWatchlistAppService appService;
    private final StockSelectionIdentityResolver identityResolver;

    public StockSelectionWatchlistController(StockSelectionWatchlistAppService appService,
                                              StockSelectionIdentityResolver identityResolver) {
        this.appService = appService;
        this.identityResolver = identityResolver;
    }

    @GetMapping
    public Result<StockSelectionWatchlistDTO.ListResponse> list(
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.list(scope(principal, tenant, signature)));
    }

    @GetMapping("/performance")
    public Result<StockSelectionWatchlistDTO.PerformanceResponse> performance(
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.performance(scope(principal, tenant, signature)));
    }

    @GetMapping("/{itemId}/performance/history")
    public Result<StockSelectionWatchlistDTO.PerformanceHistoryResponse> performanceHistory(
            @PathVariable long itemId,
            @RequestParam(value = "limit", defaultValue = "90") int limit,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.performanceHistory(scope(principal, tenant, signature), itemId, limit));
    }

    @PostMapping
    public Result<StockSelectionWatchlistDTO.Item> add(
            @RequestBody StockSelectionWatchlistDTO.AddRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.add(scope(principal, tenant, signature), request));
    }

    @PostMapping("/{itemId}/reviews")
    public Result<StockSelectionWatchlistDTO.Item> review(
            @PathVariable long itemId,
            @RequestBody StockSelectionWatchlistDTO.ReviewRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.review(scope(principal, tenant, signature), itemId, request));
    }

    private StockSelectionStrategyRunBO.Scope scope(String principal, String tenant, String signature) {
        return identityResolver.resolve(principal, tenant, signature);
    }
}
