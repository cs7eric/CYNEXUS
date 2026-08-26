package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.MarketEventReplayDTO;
import cn.cccs7.cynexus.market.application.service.EventReplayAppService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/screener/event-replay")
public class EventReplayController {
    private final EventReplayAppService appService;
    public EventReplayController(EventReplayAppService appService) { this.appService = appService; }

    @GetMapping
    public Result<MarketEventReplayDTO> replay(@RequestParam String code, @RequestParam LocalDate tradeDate,
                                                @RequestParam(required = false) OffsetDateTime anchorAt,
                                                @RequestParam(required = false) Integer minuteLimit) {
        return Result.success(appService.replay(code, tradeDate, anchorAt, minuteLimit));
    }
}
