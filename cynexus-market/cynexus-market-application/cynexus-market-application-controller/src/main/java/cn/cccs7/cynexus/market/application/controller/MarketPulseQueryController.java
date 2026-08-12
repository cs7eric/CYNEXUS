package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketPulseHeatmapDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexKlineBatchDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseLimitEmotionDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseManualFundFlowDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewEltdxDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorConstituentsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorListDTO;
import cn.cccs7.cynexus.market.api.request.MarketPulseManualFundFlowRequest;
import cn.cccs7.cynexus.market.application.service.MarketPulseQueryAppService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/pulse")
public class MarketPulseQueryController {

    private final MarketPulseQueryAppService marketPulseQueryAppService;

    public MarketPulseQueryController(MarketPulseQueryAppService marketPulseQueryAppService) {
        this.marketPulseQueryAppService = marketPulseQueryAppService;
    }

    @GetMapping("/market-overview-akshare")
    public Result<MarketPulseOverviewDTO> getMarketOverviewAkshare() {
        return Result.success(marketPulseQueryAppService.getOverviewAkshare());
    }

    @PostMapping("/market-overview-akshare/refresh")
    public Result<MarketPulseOverviewDTO> refreshMarketOverviewAkshare() {
        return Result.success(marketPulseQueryAppService.getOverviewAkshare());
    }

    @GetMapping("/market-overview-akshare/archive/{tradingDate}")
    public Result<MarketPulseOverviewDTO> getMarketOverviewArchive(@PathVariable String tradingDate) {
        return Result.success(marketPulseQueryAppService.getOverviewArchive(tradingDate));
    }

    @GetMapping("/market-overview-eltdx")
    public Result<MarketPulseOverviewEltdxDTO> getMarketOverviewEltdx() {
        return Result.success(marketPulseQueryAppService.getOverviewEltdx());
    }

    @PostMapping("/market-overview-eltdx/refresh")
    public Result<MarketPulseOverviewEltdxDTO> refreshMarketOverviewEltdx() {
        return Result.success(marketPulseQueryAppService.getOverviewEltdx());
    }

    @GetMapping("/market-overview-akshare/history")
    public Result<MarketPulseOverviewHistoryDTO> getMarketOverviewHistory(@RequestParam(defaultValue = "60d") String range) {
        return Result.success(marketPulseQueryAppService.getOverviewHistory(range));
    }

    @GetMapping("/sector-snapshot/latest")
    public Result<MarketPulseSectorSnapshotDTO> getLatestSectorSnapshot() {
        return Result.success(marketPulseQueryAppService.getLatestSectorSnapshot());
    }

    @GetMapping("/sector-snapshot/history")
    public Result<MarketPulseSectorSnapshotHistoryDTO> getSectorSnapshotHistory(@RequestParam(defaultValue = "120") int days) {
        return Result.success(marketPulseQueryAppService.getSectorSnapshotHistory(days));
    }

    @GetMapping("/market-overview-manual-fund-flow")
    public ResponseEntity<Result<MarketPulseManualFundFlowDTO>> getManualFundFlow(@RequestParam String tradingDate) {
        return marketPulseQueryAppService.getManualFundFlow(LocalDate.parse(tradingDate))
                .map(payload -> ResponseEntity.ok(Result.success(payload)))
                .orElseGet(() -> ResponseEntity.status(404).body(Result.success(null)));
    }

    @PostMapping("/market-overview-manual-fund-flow")
    public Result<MarketPulseManualFundFlowDTO> saveManualFundFlow(@RequestBody MarketPulseManualFundFlowRequest request) {
        return Result.success(marketPulseQueryAppService.saveManualFundFlow(request));
    }

    @GetMapping("/index-returns")
    public Result<MarketPulseIndexReturnsDTO> getIndexReturns(@RequestParam(defaultValue = "5") int days) {
        return Result.success(marketPulseQueryAppService.getIndexReturns(days));
    }

    @GetMapping("/index-returns/history")
    public Result<MarketPulseIndexReturnsHistoryDTO> getIndexReturnsHistory(@RequestParam(defaultValue = "5") int window,
                                                                            @RequestParam String start,
                                                                            @RequestParam String end) {
        return Result.success(marketPulseQueryAppService.getIndexReturnsHistory(window, LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/index-kline/batch")
    public Result<MarketPulseIndexKlineBatchDTO> getIndexKlineBatch(@RequestParam(required = false) String codes,
                                                                    @RequestParam String date,
                                                                    @RequestParam(defaultValue = "1m") String interval) {
        List<String> codeList = codes == null || codes.isBlank()
                ? List.of()
                : Arrays.stream(codes.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
        return Result.success(marketPulseQueryAppService.getIndexKlineBatch(codeList, LocalDate.parse(date), interval));
    }

    @GetMapping("/style-sectors")
    public Result<MarketPulseStyleSectorListDTO> getStyleSectors() {
        return Result.success(marketPulseQueryAppService.getStyleSectors());
    }

    @GetMapping("/style-sectors/{name}/constituents")
    public Result<MarketPulseStyleSectorConstituentsDTO> getStyleSectorConstituents(@PathVariable String name) {
        return Result.success(marketPulseQueryAppService.getStyleSectorConstituents(name));
    }

    @GetMapping("/industries/{code}/constituents")
    public Result<MarketPulseStyleSectorConstituentsDTO> getIndustryConstituents(@PathVariable String code) {
        return Result.success(marketPulseQueryAppService.getIndustryConstituents(code));
    }

    @GetMapping("/heatmap")
    public Result<MarketPulseHeatmapDTO> getHeatmap(@RequestParam(defaultValue = "industries") String kind,
                                                    @RequestParam(defaultValue = "200") int top_n) {
        return Result.success(marketPulseQueryAppService.getHeatmap(kind));
    }

    @GetMapping("/limit-emotion")
    public Result<MarketPulseLimitEmotionDTO> getLimitEmotion(@RequestParam(required = false) String date) {
        return Result.success(marketPulseQueryAppService.getLimitEmotion(date == null || date.isBlank() ? null : LocalDate.parse(date)));
    }

    @PostMapping("/limit-emotion/refresh")
    public Result<MarketPulseLimitEmotionDTO> refreshLimitEmotion() {
        return Result.success(marketPulseQueryAppService.getLimitEmotion());
    }
}
