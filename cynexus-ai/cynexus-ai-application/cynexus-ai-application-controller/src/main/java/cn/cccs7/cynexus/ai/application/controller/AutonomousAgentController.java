package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentStreamEventDTO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.application.service.AgentIdentityResolver;
import cn.cccs7.cynexus.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/ai/agent")
public class AutonomousAgentController {

    private final AutonomousAgentAppService agentAppService;
    private final AgentIdentityResolver identityResolver;

    public AutonomousAgentController(AutonomousAgentAppService agentAppService, AgentIdentityResolver identityResolver) {
        this.agentAppService = agentAppService;
        this.identityResolver = identityResolver;
    }

    @PostMapping("/runs")
    public Result<AgentRunDTO> run(@RequestBody AgentRunRequest request, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.execute(request, identity(httpRequest), ignored -> { }));
    }

    @PostMapping(value = "/runs/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestBody AgentRunRequest request, HttpServletRequest httpRequest) {
        AgentIdentity agentIdentity = identity(httpRequest);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        CompletableFuture.runAsync(() -> {
            try {
                agentAppService.execute(request, agentIdentity, event -> send(emitter, event));
                emitter.complete();
            } catch (RuntimeException exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @PostMapping("/runs/{runNo}/cancel")
    public Result<Boolean> cancel(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.cancel(identity(httpRequest), runNo));
    }

    @GetMapping("/runs/{runNo}")
    public Result<AgentRunDTO> getRun(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.getRun(identity(httpRequest), runNo));
    }

    @GetMapping("/tools")
    public Result<List<AgentToolDefinitionDTO>> tools() {
        return Result.success(agentAppService.enabledTools());
    }

    @GetMapping("/conversations")
    public Result<List<AgentConversationDTO>> conversations(HttpServletRequest httpRequest) {
        return Result.success(agentAppService.conversations(identity(httpRequest), 30));
    }

    @GetMapping("/conversations/{conversationNo}/messages")
    public Result<List<AgentMessageDTO>> messages(@PathVariable String conversationNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.messages(identity(httpRequest), conversationNo));
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

    private AgentIdentity identity(HttpServletRequest request) {
        try {
            return identityResolver.resolve(request.getHeader("X-Cynexus-Principal"),
                    request.getHeader("X-Cynexus-Tenant"), request.getHeader("X-Cynexus-Identity-Signature"));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception);
        }
    }
}
