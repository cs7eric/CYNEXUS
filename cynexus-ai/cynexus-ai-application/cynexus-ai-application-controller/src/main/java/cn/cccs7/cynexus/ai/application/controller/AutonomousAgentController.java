package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.AutonomousAgentAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/agent")
public class AutonomousAgentController {

    private final AutonomousAgentAppService agentAppService;

    public AutonomousAgentController(AutonomousAgentAppService agentAppService) {
        this.agentAppService = agentAppService;
    }

    @PostMapping("/runs")
    public Result<AgentRunDTO> run(@RequestBody AgentRunRequest request) {
        return Result.success(agentAppService.execute(request));
    }

    @GetMapping("/runs/{runNo}")
    public Result<AgentRunDTO> getRun(@PathVariable String runNo,
                                     @RequestParam(defaultValue = "anonymous") String ownerKey) {
        return Result.success(agentAppService.getRun(ownerKey, runNo));
    }

    @GetMapping("/tools")
    public Result<List<AgentToolDefinitionDTO>> tools() {
        return Result.success(agentAppService.enabledTools());
    }

    @GetMapping("/conversations")
    public Result<List<AgentConversationDTO>> conversations(
            @RequestParam(defaultValue = "anonymous") String ownerKey,
            @RequestParam(defaultValue = "30") Integer limit) {
        return Result.success(agentAppService.conversations(ownerKey, limit == null ? 30 : limit));
    }

    @GetMapping("/conversations/{conversationNo}/messages")
    public Result<List<AgentMessageDTO>> messages(@PathVariable String conversationNo,
                                                  @RequestParam(defaultValue = "anonymous") String ownerKey) {
        return Result.success(agentAppService.messages(ownerKey, conversationNo));
    }
}
