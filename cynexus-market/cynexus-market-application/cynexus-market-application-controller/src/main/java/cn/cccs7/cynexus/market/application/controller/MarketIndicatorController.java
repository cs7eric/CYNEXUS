package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketIndicatorDTO;
import cn.cccs7.cynexus.market.application.service.MarketIndicatorQueryAppService;
import cn.cccs7.cynexus.market.application.service.MarketIndicatorWatchlistAppService;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/indicator")
public class MarketIndicatorController {
    private final MarketIndicatorQueryAppService service;
    private final MarketIndicatorWatchlistAppService watchlistService;

    public MarketIndicatorController(MarketIndicatorQueryAppService service, MarketIndicatorWatchlistAppService watchlistService) {
        this.service = service;
        this.watchlistService = watchlistService;
    }

    @GetMapping("/latest")
    public Result<MarketIndicatorDTO.Overview> latest(
            @RequestParam(required = false) String tradeDate,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String eventType,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(service.latest(tradeDate, status, eventType, limit));
    }

    @GetMapping("/industry/detail")
    public Result<MarketIndicatorDTO.IndustryDetail> industryDetail(
            @RequestParam String industryCode,
            @RequestParam(required = false) String tradeDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(service.industryDetail(tradeDate, industryCode, page, pageSize));
    }

    @GetMapping("/transmission")
    public Result<java.util.Map<String, Object>> transmission(
            @RequestParam(required = false) String tradeDate,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(service.transmission(tradeDate, limit));
    }

    @PostMapping("/watchlist/preview")
    public Result<Map<String, Object>> watchlistPreview(@RequestBody Map<String, Object> request) {
        return Result.success(watchlistService.preview(request));
    }

    @PostMapping("/watchlist/confirm")
    public Result<Map<String, Object>> watchlistConfirm(@RequestBody Map<String, Object> request) {
        return Result.success(watchlistService.confirm(request));
    }
}
