package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioAccountDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioPerformanceDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioTradeAnalyticsDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioAccountUpdateRequest;
import cn.cccs7.cynexus.market.api.request.PortfolioTradeUpdateRequest;
import cn.cccs7.cynexus.market.api.request.PortfolioCashFlowRequest;
import cn.cccs7.cynexus.market.api.request.PortfolioAccountSnapshotRequest;
import cn.cccs7.cynexus.market.application.service.PortfolioOverviewAppService;
import cn.cccs7.cynexus.market.application.service.PortfolioTradeAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/portfolio")
public class PortfolioOverviewController {
    private final PortfolioOverviewAppService service;
    private final PortfolioTradeAppService tradeService;

    public PortfolioOverviewController(PortfolioOverviewAppService service, PortfolioTradeAppService tradeService) {
        this.service = service;
        this.tradeService = tradeService;
    }

    @GetMapping("/overview")
    public Result<PortfolioOverviewDTO> overview() {
        return Result.success(service.overview());
    }

    @GetMapping("/account")
    public Result<PortfolioAccountDTO> account() {
        return Result.success(service.account());
    }

    @PatchMapping("/account")
    public Result<PortfolioAccountDTO> updateAccount(@RequestBody PortfolioAccountUpdateRequest request) {
        return Result.success(service.updateAccount(request));
    }

    @GetMapping("/trades")
    public Result<List<PortfolioTradeDTO>> trades(@RequestParam(required = false) String symbol) {
        return Result.success(service.trades(symbol));
    }

    @PatchMapping("/trades/{tradeId}")
    public Result<PortfolioTradeDTO> updateTrade(@org.springframework.web.bind.annotation.PathVariable long tradeId,
                                                 @RequestBody PortfolioTradeUpdateRequest request) {
        return Result.success(tradeService.update(tradeId, request));
    }

    @GetMapping("/performance")
    public Result<PortfolioPerformanceDTO> performance(@RequestParam(required = false) String range,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to) {
        return Result.success(service.performance(range, from, to));
    }

    @GetMapping("/analytics")
    public Result<PortfolioTradeAnalyticsDTO> analytics(@RequestParam(required = false) String from,
                                                        @RequestParam(required = false) String to,
                                                        @RequestParam(required = false) String symbol,
                                                        @RequestParam(required = false) String instrumentType,
                                                        @RequestParam(required = false) String result,
                                                        @RequestParam(required = false) String holdingPeriod,
                                                        @RequestParam(required = false) String strategy,
                                                        @RequestParam(required = false) String tag) {
        return Result.success(service.analytics(from, to, symbol, instrumentType, result, holdingPeriod, strategy, tag));
    }

    @GetMapping("/cash-flows") public Result<List<cn.cccs7.cynexus.market.api.dto.PortfolioCashFlowDTO>> cashFlows(){ return Result.success(service.cashFlows()); }
    @org.springframework.web.bind.annotation.PostMapping("/cash-flows") public Result<cn.cccs7.cynexus.market.api.dto.PortfolioCashFlowDTO> addCashFlow(@RequestBody PortfolioCashFlowRequest r){ return Result.success(service.addCashFlow(r)); }
    @GetMapping("/account-snapshots") public Result<List<cn.cccs7.cynexus.market.api.dto.PortfolioAccountSnapshotDTO>> snapshots(){ return Result.success(service.snapshots()); }
    @GetMapping("/industry-concentration") public Result<List<java.util.Map<String,Object>>> industryConcentration(){ return Result.success(service.industryConcentration()); }
    @org.springframework.web.bind.annotation.PostMapping("/account-snapshots") public Result<cn.cccs7.cynexus.market.api.dto.PortfolioAccountSnapshotDTO> addSnapshot(@RequestBody PortfolioAccountSnapshotRequest r){ return Result.success(service.addSnapshot(r)); }
}
