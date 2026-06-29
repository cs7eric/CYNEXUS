package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentIndexDailyDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentLimitEmotionSummaryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMaCountDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMsiIndexDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentProfitEffectDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentRiskAppetiteDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentSectorBreadthDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentStyleRiskDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentTurnoverDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentVolatilityDTO;
import cn.cccs7.cynexus.market.application.service.MarketSentimentQueryAppService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/sentiment")
public class MarketSentimentQueryController {

    private final MarketSentimentQueryAppService marketSentimentQueryAppService;

    public MarketSentimentQueryController(MarketSentimentQueryAppService marketSentimentQueryAppService) {
        this.marketSentimentQueryAppService = marketSentimentQueryAppService;
    }

    @GetMapping("/ma-count")
    public Result<MarketSentimentMaCountDTO> getMaCount(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getMaCount(parseDate(date)));
    }

    @GetMapping("/ma-count/history")
    public Result<MarketSentimentMaCountDTO.HistoryResponseDTO> getMaCountHistory(@RequestParam String start,
                                                                                  @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getMaCountHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/risk-appetite")
    public Result<MarketSentimentRiskAppetiteDTO> getRiskAppetite(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getRiskAppetite(parseDate(date)));
    }

    @GetMapping("/risk-appetite/history")
    public Result<MarketSentimentRiskAppetiteDTO.HistoryResponseDTO> getRiskAppetiteHistory(@RequestParam String start,
                                                                                            @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getRiskAppetiteHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/limit-emotion-summary")
    public Result<MarketSentimentLimitEmotionSummaryDTO> getLimitEmotion(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getLimitEmotion(parseDate(date)));
    }

    @GetMapping("/limit-emotion-summary/history")
    public Result<MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO> getLimitEmotionHistory(@RequestParam String start,
                                                                                                   @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getLimitEmotionHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/volatility-sentiment")
    public Result<MarketSentimentVolatilityDTO> getVolatility(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getVolatility(parseDate(date)));
    }

    @GetMapping("/volatility-sentiment/history")
    public Result<MarketSentimentVolatilityDTO.HistoryResponseDTO> getVolatilityHistory(@RequestParam String start,
                                                                                        @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getVolatilityHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/turnover-activity")
    public Result<MarketSentimentTurnoverDTO> getTurnover(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getTurnover(parseDate(date)));
    }

    @GetMapping("/turnover-activity/history")
    public Result<MarketSentimentTurnoverDTO.HistoryResponseDTO> getTurnoverHistory(@RequestParam String start,
                                                                                    @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getTurnoverHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/style-risk-appetite")
    public Result<MarketSentimentStyleRiskDTO> getStyleRisk(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getStyleRisk(parseDate(date)));
    }

    @GetMapping("/style-risk-appetite/history")
    public Result<MarketSentimentStyleRiskDTO.HistoryResponseDTO> getStyleRiskHistory(@RequestParam String start,
                                                                                      @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getStyleRiskHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/profit-effect")
    public Result<MarketSentimentProfitEffectDTO> getProfitEffect(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getProfitEffect(parseDate(date)));
    }

    @GetMapping("/profit-effect/history")
    public Result<MarketSentimentProfitEffectDTO.HistoryResponseDTO> getProfitEffectHistory(@RequestParam String start,
                                                                                            @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getProfitEffectHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/sector-breadth")
    public Result<MarketSentimentSectorBreadthDTO> getSectorBreadth(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getSectorBreadth(parseDate(date)));
    }

    @GetMapping("/sector-breadth/history")
    public Result<MarketSentimentSectorBreadthDTO.HistoryResponseDTO> getSectorBreadthHistory(@RequestParam(defaultValue = "30") int days,
                                                                                              @RequestParam(required = false) String end) {
        return Result.success(marketSentimentQueryAppService.getSectorBreadthHistory(days, parseDate(end)));
    }

    @GetMapping("/index")
    public Result<MarketSentimentMsiIndexDTO> getMsiIndex(@RequestParam(required = false) String date) {
        return Result.success(marketSentimentQueryAppService.getMsiIndex(parseDate(date)));
    }

    @GetMapping("/index/history")
    public Result<MarketSentimentMsiIndexDTO.HistoryResponseDTO> getMsiIndexHistory(@RequestParam String start,
                                                                                    @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getMsiIndexHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/market-overview/history")
    public Result<MarketPulseOverviewHistoryDTO> getMarketOverviewHistory(@RequestParam String start,
                                                                          @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getOverviewHistory(LocalDate.parse(start), LocalDate.parse(end)));
    }

    @GetMapping("/index/daily")
    public Result<MarketSentimentIndexDailyDTO> getIndexDaily(@RequestParam String code,
                                                              @RequestParam String start,
                                                              @RequestParam String end) {
        return Result.success(marketSentimentQueryAppService.getIndexDaily(code, LocalDate.parse(start), LocalDate.parse(end)));
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
