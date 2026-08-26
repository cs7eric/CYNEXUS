package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.application.service.StockSelectionStrategyCatalogAppService;
import cn.cccs7.cynexus.market.application.service.StockSelectionStrategyBacktestAppService;
import cn.cccs7.cynexus.market.application.service.StockSelectionStrategyHealthAppService;
import cn.cccs7.cynexus.market.application.service.StockSelectionStrategyRunAppService;
import cn.cccs7.cynexus.market.application.service.stockselection.StockSelectionIdentityResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener/strategies")
public class StockSelectionStrategyController {

    private final StockSelectionStrategyCatalogAppService appService;
    private final StockSelectionStrategyRunAppService runAppService;
    private final StockSelectionStrategyBacktestAppService backtestAppService;
    private final StockSelectionStrategyHealthAppService healthAppService;
    private final StockSelectionIdentityResolver identityResolver;

    public StockSelectionStrategyController(StockSelectionStrategyCatalogAppService appService,
                                            StockSelectionStrategyRunAppService runAppService,
                                            StockSelectionStrategyBacktestAppService backtestAppService,
                                            StockSelectionStrategyHealthAppService healthAppService,
                                            StockSelectionIdentityResolver identityResolver) {
        this.appService = appService;
        this.runAppService = runAppService;
        this.backtestAppService = backtestAppService;
        this.healthAppService = healthAppService;
        this.identityResolver = identityResolver;
    }

    @GetMapping
    public Result<StockSelectionStrategyDTO.CatalogResponse> listActive(
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.listActive(identityResolver.resolve(principal, tenant, signature)));
    }

    @PostMapping
    public Result<StockSelectionStrategyDTO.Strategy> create(
            @RequestBody StockSelectionStrategyDTO.SaveRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.create(identityResolver.resolve(principal, tenant, signature), request));
    }

    @PutMapping("/{strategyCode}")
    public Result<StockSelectionStrategyDTO.Strategy> update(
            @PathVariable String strategyCode,
            @RequestBody StockSelectionStrategyDTO.SaveRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(appService.update(identityResolver.resolve(principal, tenant, signature), strategyCode, request));
    }

    @PostMapping("/{strategyCode}/runs")
    public Result<StockSelectionStrategyDTO.RunResponse> run(
            @PathVariable String strategyCode,
            @RequestParam(required = false) String tradeDate,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(runAppService.run(identityResolver.resolve(principal, tenant, signature), strategyCode, tradeDate));
    }

    @GetMapping("/{strategyCode}/health")
    public Result<StockSelectionStrategyDTO.HealthResponse> health(
            @PathVariable String strategyCode,
            @RequestParam(required = false) String tradeDate,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(healthAppService.assess(identityResolver.resolve(principal, tenant, signature), strategyCode, tradeDate));
    }

    @PostMapping("/{strategyCode}/backtests")
    public Result<StockSelectionStrategyDTO.BacktestResponse> backtest(
            @PathVariable String strategyCode,
            @RequestBody(required = false) StockSelectionStrategyDTO.BacktestRequest request,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        return Result.success(backtestAppService.queue(identityResolver.resolve(principal, tenant, signature), strategyCode, request));
    }

}
