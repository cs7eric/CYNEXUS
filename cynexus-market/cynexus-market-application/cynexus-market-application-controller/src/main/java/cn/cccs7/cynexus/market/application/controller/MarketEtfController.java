package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEtfDTO;
import cn.cccs7.cynexus.market.application.service.MarketEtfQueryAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/etf")
public class MarketEtfController {

    private final MarketEtfQueryAppService appService;

    public MarketEtfController(MarketEtfQueryAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<MarketEtfDTO> getMarket(@RequestParam(defaultValue = "10") int historyDays,
                                          @RequestParam(defaultValue = "180") int rowLimit) {
        return Result.success(appService.getMarket(historyDays, rowLimit));
    }
}
