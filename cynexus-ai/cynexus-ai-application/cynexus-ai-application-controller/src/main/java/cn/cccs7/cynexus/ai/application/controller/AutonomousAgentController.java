package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentStreamEventDTO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.AutonomousAgentAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

    @PostMapping(value = "/runs/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestBody AgentRunRequest request) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        CompletableFuture.runAsync(() -> {
            try {
                agentAppService.execute(request, event -> send(emitter, event));
                emitter.complete();
            } catch (RuntimeException exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @PostMapping("/runs/{runNo}/cancel")
    public Result<Boolean> cancel(@PathVariable String runNo,
                                  @RequestParam(defaultValue = "anonymous") String ownerKey) {
        return Result.success(agentAppService.cancel(ownerKey, runNo));
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

    private void send(SseEmitter emitter, AgentRunEventBO event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(AgentStreamEventDTO.builder()
                    .type(event.getType()).runNo(event.getRunNo()).status(event.getStatus()).stepNo(event.getStepNo())
                    .toolCode(event.getToolCode()).summary(event.getSummary()).answer(event.getAnswer())
                    .errorMessage(event.getErrorMessage()).build()));
        } catch (Exception exception) {
            throw new IllegalStateException("Agent stream disconnected", exception);
        }
    }
}
