package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.application.service.MarketIndicatorReplayAiAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/market-indicator")
public class MarketIndicatorReplayAiController {
    private final MarketIndicatorReplayAiAppService appService;

    public MarketIndicatorReplayAiController(MarketIndicatorReplayAiAppService appService) {
        this.appService = appService;
    }

    @PostMapping("/replay-explanation")
    public Result<Map<String, Object>> explain(@RequestBody Map<String, Object> evidence) {
        try {
            return Result.success(appService.explain(evidence));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            return Result.fail("AI_PROVIDER_ERROR", exception.getMessage());
        }
    }
}
