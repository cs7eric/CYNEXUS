package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentStreamEventDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentRunExportDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
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
                agentAppService.stream(request, agentIdentity, event -> send(emitter, event));
                emitter.complete();
            } catch (RuntimeException exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping(value = "/runs/{runNo}/stream", produces = "text/event-stream")
    public SseEmitter reconnect(@PathVariable String runNo,
                                @RequestParam(required = false) Integer afterEventNo,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
                                HttpServletRequest httpRequest) {
        AgentIdentity agentIdentity = identity(httpRequest);
        int cursor = afterEventNo == null ? parseEventSequence(lastEventId) : Math.max(0, afterEventNo);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        CompletableFuture.runAsync(() -> {
            try {
                agentAppService.replayStream(agentIdentity, runNo, cursor, event -> send(emitter, event));
                emitter.complete();
            } catch (RuntimeException exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/runs/{runNo}/events")
    public Result<List<AgentStreamEventDTO>> events(@PathVariable String runNo,
                                                    @RequestParam(defaultValue = "0") int afterEventNo,
                                                    @RequestParam(defaultValue = "200") int limit,
                                                    HttpServletRequest httpRequest) {
        return Result.success(agentAppService.events(identity(httpRequest), runNo, afterEventNo, limit).stream()
                .map(this::toEventDTO).toList());
    }

    @PostMapping("/runs/{runNo}/cancel")
    public Result<Boolean> cancel(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.cancel(identity(httpRequest), runNo));
    }

    @PostMapping("/runs/{runNo}/retry")
    public Result<AgentRunDTO> retry(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.retry(identity(httpRequest), runNo));
    }

    @PostMapping("/runs/{runNo}/regenerate")
    public Result<AgentRunDTO> regenerate(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.regenerate(identity(httpRequest), runNo));
    }

    @GetMapping("/runs/{runNo}")
    public Result<AgentRunDTO> getRun(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.getRun(identity(httpRequest), runNo));
    }

    @GetMapping("/runs/{runNo}/export")
    public Result<AgentRunExportDTO> export(@PathVariable String runNo,
                                            @RequestParam(defaultValue = "markdown") String format,
                                            HttpServletRequest httpRequest) {
        return Result.success(agentAppService.export(identity(httpRequest), runNo, format));
    }

    @GetMapping("/tools")
    public Result<List<AgentToolDefinitionDTO>> tools() {
        return Result.success(agentAppService.enabledTools());
    }

    @GetMapping("/conversations")
    public Result<List<AgentConversationDTO>> conversations(@RequestParam(required = false) String query,
                                                            HttpServletRequest httpRequest) {
        return Result.success(agentAppService.conversations(identity(httpRequest), 30, query));
    }

    @GetMapping("/conversations/{conversationNo}/messages")
    public Result<List<AgentMessageDTO>> messages(@PathVariable String conversationNo, HttpServletRequest httpRequest) {
        return Result.success(agentAppService.messages(identity(httpRequest), conversationNo));
    }

    private void send(SseEmitter emitter, AgentRunEventBO event) {
        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event().name(event.getType())
                    .data(toEventDTO(event));
            if (event.getSequenceNo() != null) builder.id(String.valueOf(event.getSequenceNo()));
            emitter.send(builder);
        } catch (Exception exception) {
            throw new IllegalStateException("Agent stream disconnected", exception);
        }
    }

    private AgentStreamEventDTO toEventDTO(AgentRunEventBO event) {
        return AgentStreamEventDTO.builder().sequenceNo(event.getSequenceNo()).type(event.getType())
                .runNo(event.getRunNo()).status(event.getStatus()).stepNo(event.getStepNo())
                .toolCode(event.getToolCode()).summary(event.getSummary()).answer(event.getAnswer())
                .delta(event.getDelta()).errorMessage(event.getErrorMessage())
                .occurredAt(event.getOccurredAt()).build();
    }

    private int parseEventSequence(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
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
