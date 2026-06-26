package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiPromptTemplateDTO;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/prompts")
public class AiPromptController {

    @GetMapping("/templates")
    public Result<List<AiPromptTemplateDTO>> listPromptTemplates() {
        return Result.success(List.of(new AiPromptTemplateDTO("market_daily_review",
                "Summarize market information with JSON output.")));
    }
}
