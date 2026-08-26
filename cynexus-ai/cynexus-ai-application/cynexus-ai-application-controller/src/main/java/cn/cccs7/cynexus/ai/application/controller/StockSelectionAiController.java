package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.StockSelectionAiDTO;
import cn.cccs7.cynexus.ai.api.request.StockSelectionAiRequest;
import cn.cccs7.cynexus.ai.application.service.StockSelectionAiAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/stock-selection")
public class StockSelectionAiController {

    private final StockSelectionAiAppService appService;

    public StockSelectionAiController(StockSelectionAiAppService appService) {
        this.appService = appService;
    }

    @PostMapping("/screening")
    public Result<StockSelectionAiDTO.AnalysisResponse> analyze(@RequestBody StockSelectionAiRequest.AnalyzeRequest request) {
        try {
            return Result.success(appService.analyze(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            return Result.fail("AI_PROVIDER_ERROR", exception.getMessage());
        }
    }

    @PostMapping("/intent-plan")
    public Result<StockSelectionAiDTO.IntentPlanResponse> parseIntent(@RequestBody StockSelectionAiRequest.IntentPlanRequest request) {
        try {
            return Result.success(appService.parseIntent(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
