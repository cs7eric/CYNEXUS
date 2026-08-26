package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEventReplayDTO;
import cn.cccs7.cynexus.market.application.service.EventReplayAppService;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Canonical research route; legacy Screener replay remains available during the compatibility window. */
@RestController
@RequestMapping("/api/market/screener/factor-research/events")
public class MarketFactorResearchReplayController {
    private final EventReplayAppService appService;
    public MarketFactorResearchReplayController(EventReplayAppService appService) { this.appService = appService; }

    @GetMapping("/{tradeDate}/{code}/replay")
    public Result<MarketEventReplayDTO> replay(@PathVariable LocalDate tradeDate, @PathVariable String code,
                                                @RequestParam String eventType,
                                                @RequestParam(required = false) OffsetDateTime anchorAt,
                                                @RequestParam(required = false) Integer minuteLimit) {
        // Event definition selection is validated here even though the raw replay can be used by each registered provider.
        MarketFactorResearchBO.EventType.valueOf(eventType);
        return Result.success(appService.replay(code, tradeDate, anchorAt, minuteLimit));
    }
}
