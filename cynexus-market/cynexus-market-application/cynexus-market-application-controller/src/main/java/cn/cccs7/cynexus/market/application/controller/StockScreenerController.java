package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.StockScreenerDTO;
import cn.cccs7.cynexus.market.application.service.StockScreenerAppService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener")
public class StockScreenerController {

    private final StockScreenerAppService stockScreenerAppService;

    public StockScreenerController(StockScreenerAppService stockScreenerAppService) {
        this.stockScreenerAppService = stockScreenerAppService;
    }

    @GetMapping
    public Result<StockScreenerDTO.Response> screen(@RequestParam(required = false) String tradeDate,
                                                     @RequestParam(required = false) String universe,
                                                     @RequestParam(required = false) Boolean excludeSt,
                                                     @RequestParam(required = false) Boolean excludeChiNext,
                                                     @RequestParam(required = false) Boolean excludeStar,
                                                     @RequestParam(required = false) BigDecimal minPrice,
                                                     @RequestParam(required = false) BigDecimal maxPrice,
                                                     @RequestParam(required = false) BigDecimal minChangePct,
                                                     @RequestParam(required = false) BigDecimal maxChangePct,
                                                     @RequestParam(required = false) BigDecimal minAmount,
                                                     @RequestParam(required = false) BigDecimal minMarketCap,
                                                     @RequestParam(required = false) BigDecimal maxMarketCap,
                                                     @RequestParam(required = false) String industry,
                                                     @RequestParam(required = false) String tdxIndustryCode,
                                                     @RequestParam(required = false) String thsIndustryCode,
                                                     @RequestParam(required = false) String conceptSectorId,
                                                     @RequestParam(required = false) String styleSectorId,
                                                     @RequestParam(required = false) String board,
                                                     @RequestParam(required = false) BigDecimal minAmplitudePct,
                                                     @RequestParam(required = false) BigDecimal maxAmplitudePct,
                                                     @RequestParam(required = false) BigDecimal minReturnPct5d,
                                                     @RequestParam(required = false) BigDecimal maxReturnPct5d,
                                                     @RequestParam(required = false) BigDecimal minReturnPct10d,
                                                     @RequestParam(required = false) BigDecimal maxReturnPct10d,
                                                     @RequestParam(required = false) BigDecimal minReturnPct30d,
                                                     @RequestParam(required = false) BigDecimal maxReturnPct30d,
                                                     @RequestParam(required = false) BigDecimal minReturnPct60d,
                                                     @RequestParam(required = false) BigDecimal maxReturnPct60d,
                                                     @RequestParam(required = false) BigDecimal minReturnPct252d,
                                                     @RequestParam(required = false) BigDecimal maxReturnPct252d,
                                                     @RequestParam(required = false) BigDecimal minMainNet,
                                                     @RequestParam(required = false) BigDecimal minMainNet5d,
                                                     @RequestParam(required = false) BigDecimal minTurnoverPct,
                                                     @RequestParam(required = false) BigDecimal maxTurnoverPct,
                                                     @RequestParam(required = false) BigDecimal minVolumeRatio,
                                                     @RequestParam(required = false) BigDecimal maxVolumeRatio,
                                                     @RequestParam(required = false) BigDecimal minPe,
                                                     @RequestParam(required = false) BigDecimal maxPe,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(required = false) String sortDir,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer pageSize,
                                                     @RequestParam(required = false) Integer volumeContractionDays,
                                                     @RequestParam(required = false) BigDecimal minBreakoutVolumeMultiple,
                                                     @RequestParam(required = false) BigDecimal maxBreakoutVolumeMultiple,
                                                     @RequestParam(required = false) BigDecimal minBullishBodyPct,
                                                     @RequestParam(required = false) BigDecimal minCloseAboveMa20Pct,
                                                     @RequestParam(required = false) BigDecimal minMa5AboveMa20Pct,
                                                     @RequestParam(required = false) Integer breakoutHighDays,
                                                     @RequestParam(required = false) BigDecimal maxAtrPct20,
                                                     @RequestParam(required = false) Integer minConsecutiveBullishDays,
                                                     @RequestParam(required = false) BigDecimal minVolumeVsMa5Multiple,
                                                     @RequestParam(required = false) BigDecimal maxVolumeVsMa5Multiple,
                                                     @RequestParam(required = false) String candlestickPattern) {
        return Result.success(stockScreenerAppService.screen(tradeDate, universe, excludeSt, excludeChiNext, excludeStar,
                minPrice, maxPrice, minChangePct, maxChangePct, minAmount, minMarketCap, maxMarketCap,
                industry, tdxIndustryCode, thsIndustryCode, conceptSectorId, styleSectorId, board, minAmplitudePct, maxAmplitudePct, minReturnPct5d, maxReturnPct5d,
                minReturnPct10d, maxReturnPct10d, minReturnPct30d, maxReturnPct30d, minReturnPct60d,
                maxReturnPct60d, minReturnPct252d, maxReturnPct252d, minMainNet, minMainNet5d,
                minTurnoverPct, maxTurnoverPct, minVolumeRatio, maxVolumeRatio, minPe, maxPe, sort, sortDir, page, pageSize,
                volumeContractionDays, minBreakoutVolumeMultiple, maxBreakoutVolumeMultiple, minBullishBodyPct, minCloseAboveMa20Pct,
                minMa5AboveMa20Pct, breakoutHighDays, maxAtrPct20, minConsecutiveBullishDays, minVolumeVsMa5Multiple,
                maxVolumeVsMa5Multiple, candlestickPattern));
    }

    @GetMapping("/industries")
    public Result<java.util.List<StockScreenerDTO.IndustryOption>> industries(@RequestParam(required = false) String tradeDate) {
        return Result.success(stockScreenerAppService.listIndustryOptions(tradeDate));
    }

    @GetMapping("/classifications")
    public Result<StockScreenerDTO.ClassificationOptions> classifications(@RequestParam(required = false) String tradeDate) {
        return Result.success(stockScreenerAppService.listClassificationOptions(tradeDate));
    }
}
