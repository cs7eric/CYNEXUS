package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.IndustryApplicationDTO;
import cn.cccs7.cynexus.market.api.request.IndustryApplicationRequest;
import cn.cccs7.cynexus.market.application.service.IndustryApplicationAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/industry-application")
public class IndustryApplicationController {

    private final IndustryApplicationAppService industryApplicationAppService;

    public IndustryApplicationController(IndustryApplicationAppService industryApplicationAppService) {
        this.industryApplicationAppService = industryApplicationAppService;
    }

    @GetMapping("/targets")
    public Result<IndustryApplicationDTO.ConfigResponse> getTargets() {
        return Result.success(industryApplicationAppService.getTargets());
    }

    @PutMapping("/targets")
    public Result<IndustryApplicationDTO.ConfigResponse> saveTargets(@RequestBody IndustryApplicationRequest.SaveTargetsRequest request) {
        return Result.success(industryApplicationAppService.saveTargets(request));
    }

    @GetMapping("/target-codes")
    public Result<IndustryApplicationDTO.TargetCodesResponse> getTargetCodes(@RequestParam(required = false) String kind) {
        return Result.success(industryApplicationAppService.listTargetCodes(kind));
    }

    @GetMapping("/kline")
    public Result<IndustryApplicationDTO.KlineResponse> getKline(@RequestParam("target_type") String targetType,
                                                                 @RequestParam String symbol,
                                                                 @RequestParam(defaultValue = "day") String period,
                                                                 @RequestParam(defaultValue = "240") int count) {
        return Result.success(industryApplicationAppService.getKline(targetType, symbol, period, count));
    }

    @PostMapping("/refresh")
    public Result<IndustryApplicationDTO.RefreshResponse> refresh(@RequestBody(required = false) IndustryApplicationRequest.RefreshRequest request) {
        return Result.success(industryApplicationAppService.refresh(request));
    }

    @GetMapping("/results/{targetId}")
    public ResponseEntity<Result<IndustryApplicationDTO.ResultResponse>> getResult(@PathVariable String targetId) {
        return industryApplicationAppService.getResult(targetId)
                .map(value -> ResponseEntity.ok(Result.success(value)))
                .orElseGet(() -> ResponseEntity.status(404).body(Result.fail("404", "industry application result not found")));
    }

    @GetMapping("/overview")
    public Result<IndustryApplicationDTO.OverviewResponse> getOverview(@RequestParam(name = "sort_by", required = false) String sortBy,
                                                                       @RequestParam(defaultValue = "false") boolean ascending,
                                                                       @RequestParam(defaultValue = "200") int count) {
        return Result.success(industryApplicationAppService.getOverview(sortBy, ascending, count));
    }

    @GetMapping("/fund-flow")
    public Result<IndustryApplicationDTO.FundFlowResponse> getFundFlow(@RequestParam(name = "trade_date", required = false) String tradeDate,
                                                                       @RequestParam(required = false) Integer top) {
        return Result.success(industryApplicationAppService.getFundFlow(tradeDate, top));
    }

    @GetMapping("/fund-flow/history")
    public Result<IndustryApplicationDTO.FundFlowHistoryResponse> getFundFlowHistory(
            @RequestParam(name = "max_trade_date", required = false) String maxTradeDate,
            @RequestParam(defaultValue = "365") int days,
            @RequestParam(required = false) Integer top) {
        return Result.success(industryApplicationAppService.getFundFlowHistory(maxTradeDate, days, top));
    }

    @PostMapping("/fund-flow/refresh")
    public Result<IndustryApplicationDTO.FundFlowResponse> refreshFundFlow(@RequestParam(name = "trade_date", required = false) String tradeDate,
                                                                          @RequestParam(required = false) Integer top) {
        return Result.success(industryApplicationAppService.getFundFlow(tradeDate, top));
    }
}
