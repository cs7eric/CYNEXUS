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

    @GetMapping("/limit-board/summaries")
    public Result<Map<String, Object>> limitBoardSummaries(
            @RequestParam(required = false) String tradeDate,
            @RequestParam(defaultValue = "all") String tradeSession,
            @RequestParam(defaultValue = "all") String direction,
            @RequestParam(defaultValue = "120") int limit) {
        return Result.success(service.limitBoardSessionSummaries(tradeDate, tradeSession, direction, limit));
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

    @GetMapping("/replay/timeline")
    public Result<MarketIndicatorDTO.ReplayTimeline> replayTimeline(
            @RequestParam(required = false) String tradeDate,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "all") String eventType,
            @RequestParam(required = false) String industryCode,
            @RequestParam(defaultValue = "3000") int limit) {
        return Result.success(service.replayTimeline(tradeDate, from, to, eventType, industryCode, limit));
    }

    @GetMapping("/radar/state")
    public Result<Map<String, Object>> radarState(@RequestParam(required = false) String tradeDate,
                                                   @RequestParam(required = false) String asOf) {
        return Result.success(service.radarState(tradeDate, asOf));
    }

    @GetMapping("/radar/events")
    public Result<Map<String, Object>> radarEvents(@RequestParam(required = false) String tradeDate,
                                                    @RequestParam(required = false) String asOf,
                                                    @RequestParam(defaultValue = "all") String kind,
                                                    @RequestParam(defaultValue = "50") int limit) {
        return Result.success(service.radarEvents(tradeDate, asOf, kind, limit));
    }

    @GetMapping("/radar/limit-ladder")
    public Result<Map<String, Object>> radarLimitLadder(@RequestParam(required = false) String tradeDate,
                                                         @RequestParam(defaultValue = "all") String board,
                                                         @RequestParam(defaultValue = "100") int limit) {
        return Result.success(service.radarLimitLadder(tradeDate, board, limit));
    }

    @GetMapping("/trade-review/market-state")
    public Result<Map<String, Object>> tradeReviewMarketState(@RequestParam(required = false) String tradeDate,
                                                               @RequestParam(required = false) String asOf,
                                                               @RequestParam(defaultValue = "union") String membership,
                                                               @RequestParam(required = false) String industryCode,
                                                               @RequestParam(defaultValue = "60") int windowDays) {
        return Result.success(service.tradeReviewMarketState(tradeDate, asOf, membership, industryCode, windowDays));
    }

    @GetMapping("/replay/summary")
    public Result<Map<String, Object>> replaySummary(@RequestParam(required = false) String tradeDate,
                                                      @RequestParam(defaultValue = "30") int limit) {
        return Result.success(service.replaySummary(tradeDate, limit));
    }

    @GetMapping("/replay/episode/{episodeId}")
    public Result<Map<String, Object>> replayEpisode(@org.springframework.web.bind.annotation.PathVariable String episodeId,
                                                      @RequestParam(required = false) String tradeDate) {
        return Result.success(service.replayEpisode(tradeDate, episodeId));
    }

    @PostMapping("/replay/explain")
    public Result<Map<String, Object>> explainReplay(@RequestBody Map<String, Object> request) {
        return Result.success(service.explainReplay(request));
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
