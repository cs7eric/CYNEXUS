package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.application.assembler.MarketOverviewAssembler;
import cn.cccs7.cynexus.market.application.service.MarketOverviewAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketOverviewController {

    private final MarketOverviewAppService marketOverviewAppService;
    private final MarketOverviewAssembler marketOverviewAssembler;

    public MarketOverviewController(MarketOverviewAppService marketOverviewAppService,
                                    MarketOverviewAssembler marketOverviewAssembler) {
        this.marketOverviewAppService = marketOverviewAppService;
        this.marketOverviewAssembler = marketOverviewAssembler;
    }

    @GetMapping("/overview")
    public Result<MarketOverviewDTO> getMarketOverview(@RequestParam("tradeDate") String tradeDate) {
        return Result.success(marketOverviewAssembler.toTarget(marketOverviewAppService.getOverview(tradeDate)));
    }
}
