package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.FuturesPositionAiDTO;
import cn.cccs7.cynexus.ai.api.request.FuturesPositionAiRequest;
import cn.cccs7.cynexus.ai.application.service.FuturesPositionAiAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/futures-position")
public class FuturesPositionAiController {

    private final FuturesPositionAiAppService appService;

    public FuturesPositionAiController(FuturesPositionAiAppService appService) {
        this.appService = appService;
    }

    @PostMapping("/analysis")
    public Result<FuturesPositionAiDTO.AnalysisResponse> analyze(
            @RequestBody FuturesPositionAiRequest.AnalyzeRequest request) {
        try {
            return Result.success(appService.analyze(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            return Result.fail("AI_PROVIDER_ERROR", exception.getMessage());
        }
    }
}
