package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.application.service.StockSelectionStrategyBacktestAppService;
import cn.cccs7.cynexus.market.application.service.stockselection.StockSelectionIdentityResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener/backtests")
public class StockSelectionBacktestController {

    private final StockSelectionStrategyBacktestAppService appService;
    private final StockSelectionIdentityResolver identityResolver;

    public StockSelectionBacktestController(StockSelectionStrategyBacktestAppService appService,
                                            StockSelectionIdentityResolver identityResolver) {
        this.appService = appService;
        this.identityResolver = identityResolver;
    }

    @GetMapping("/{backtestId}")
    public Result<StockSelectionStrategyDTO.BacktestResponse> get(
            @PathVariable long backtestId,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.find(identityResolver.resolve(principal, tenant, signature), backtestId));
    }
}
