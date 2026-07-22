package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiScenarioDTO;
import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/scenarios")
public class AiScenarioController {

    @GetMapping
    public Result<List<AiScenarioDTO>> listScenarios() {
        return Result.success(List.of(
                new AiScenarioDTO("market_daily_review", "Market Daily Review", null, null, true, true, true),
                new AiScenarioDTO(AiConstants.WORKSPACE_CHAT_CAPABILITY, "Workspace Chat", null, null, false, false, true)));
    }
}
