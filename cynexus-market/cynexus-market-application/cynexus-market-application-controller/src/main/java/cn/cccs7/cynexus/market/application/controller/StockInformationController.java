package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockInformationDTO;
import cn.cccs7.cynexus.market.application.service.StockInformationAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/stock-information")
public class StockInformationController {

    private final StockInformationAppService stockInformationAppService;

    public StockInformationController(StockInformationAppService stockInformationAppService) {
        this.stockInformationAppService = stockInformationAppService;
    }

    @GetMapping
    public Result<StockInformationDTO.FeedDTO> feed(@RequestParam String symbol,
                                                     @RequestParam(defaultValue = "50") int limit) {
        return Result.success(stockInformationAppService.feed(symbol, limit));
    }
}
