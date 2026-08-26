package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEventFactorObservatoryDTO;
import cn.cccs7.cynexus.market.application.service.MarketEventFactorObservatoryAppService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener/factor-observatory")
public class MarketEventFactorObservatoryController {
    private final MarketEventFactorObservatoryAppService appService;

    public MarketEventFactorObservatoryController(MarketEventFactorObservatoryAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<MarketEventFactorObservatoryDTO> observe(@RequestParam(required = false) String tradeDate,
                                                            @RequestParam(required = false) String cohort,
                                                            @RequestParam(required = false) BigDecimal minLargeMovePct,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) String symbol) {
        return Result.success(appService.observe(tradeDate, cohort, minLargeMovePct, limit, symbol));
    }
}
