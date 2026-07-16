package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiUsagePageDTO;
import cn.cccs7.cynexus.ai.application.service.AiUsageAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/usage")
public class AiUsageController {
    private final AiUsageAppService appService;

    public AiUsageController(AiUsageAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public Result<AiUsagePageDTO> query(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer limit) {
        return Result.success(appService.query(from, to, limit));
    }
}
