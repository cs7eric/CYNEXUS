package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockInformationContentDTO;
import cn.cccs7.cynexus.market.application.service.StockInformationContentAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/stock-information/items")
public class StockInformationContentController {

    private final StockInformationContentAppService appService;

    public StockInformationContentController(StockInformationContentAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/content")
    public Result<StockInformationContentDTO.ContentDTO> content(@RequestParam String symbol, @RequestParam String kind,
                                                                   @RequestParam String source, @RequestParam String itemId,
                                                                   @RequestParam(defaultValue = "false") boolean fetchIfMissing) {
        return Result.success(appService.content(symbol, kind, source, itemId, fetchIfMissing));
    }

    @PostMapping("/content/fetch")
    public Result<StockInformationContentDTO.ContentDTO> fetchContent(@RequestParam String symbol, @RequestParam String kind,
                                                                        @RequestParam String source, @RequestParam String itemId) {
        return Result.success(appService.fetchContent(symbol, kind, source, itemId));
    }

    @GetMapping("/analysis")
    public Result<StockInformationContentDTO.AnalysisDTO> analysis(@RequestParam String symbol, @RequestParam String kind,
                                                                     @RequestParam String source, @RequestParam String itemId) {
        return Result.success(appService.analysis(symbol, kind, source, itemId));
    }

    @PostMapping("/analysis/run")
    public Result<StockInformationContentDTO.AnalysisDTO> runAnalysis(@RequestParam String symbol, @RequestParam String kind,
                                                                        @RequestParam String source, @RequestParam String itemId) {
        return Result.success(appService.runAnalysis(symbol, kind, source, itemId));
    }
}
