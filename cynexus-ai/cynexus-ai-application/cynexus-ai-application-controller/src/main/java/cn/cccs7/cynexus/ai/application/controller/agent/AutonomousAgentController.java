package cn.cccs7.cynexus.ai.application.controller.agent;

import cn.cccs7.cynexus.ai.api.dto.AgentRunDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentToolDefinitionDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentConversationDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentMessageDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentStreamEventDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentCapabilityDTO;
import cn.cccs7.cynexus.ai.api.request.AgentCapabilityGovernanceRequest;
import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.api.dto.AgentRunExportDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentCancellationResult;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.exception.quota.AgentQuotaExceededException;
import cn.cccs7.cynexus.ai.api.request.AgentRunRequest;
import cn.cccs7.cynexus.ai.application.service.agent.execution.AutonomousAgentAppService;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.common.core.constant.TraceId;
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
        try {
            return Result.success(agentAppService.execute(request, identity(httpRequest), ignored -> { }));
        } catch (AgentQuotaExceededException exception) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), exception);
        }
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
    public Result<AgentCancellationResult> cancel(@PathVariable String runNo, HttpServletRequest httpRequest) {
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
    public Result<List<AgentToolDefinitionDTO>> tools(HttpServletRequest httpRequest) {
        identity(httpRequest);
        return Result.success(agentAppService.enabledTools());
    }

    @GetMapping("/capabilities")
    public Result<List<AgentCapabilityDTO>> capabilities(HttpServletRequest httpRequest) {
        identity(httpRequest);
        return Result.success(agentAppService.capabilities().stream().map(this::toCapabilityDTO).toList());
    }

    @PostMapping("/capabilities/{capabilityCode}/governance")
    public Result<Boolean> governCapability(@PathVariable String capabilityCode,
                                            @RequestBody AgentCapabilityGovernanceRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            identityResolver.resolveGovernance(httpRequest.getHeader("X-Cynexus-Principal"),
                    httpRequest.getHeader("X-Cynexus-Tenant"), httpRequest.getHeader("X-Cynexus-Identity-Signature"));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception);
        }
        if (request == null || request.getEnabled() == null || request.getAvailability() == null
                || request.getPriority() == null || request.getPriority() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capability governance fields are invalid");
        }
        try {
            return Result.success(agentAppService.updateCapability(capabilityCode, request.getEnabled(),
                    request.getAvailability(), request.getPriority()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
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
                .traceId(event.getTraceId())
                .runNo(event.getRunNo()).status(event.getStatus()).stepNo(event.getStepNo())
                .toolCode(event.getToolCode()).summary(event.getSummary()).answer(event.getAnswer())
                .delta(event.getDelta()).errorMessage(event.getErrorMessage())
                .occurredAt(event.getOccurredAt()).build();
    }

    private AgentCapabilityDTO toCapabilityDTO(AgentCapabilityDescriptor item) {
        return AgentCapabilityDTO.builder().id(item.getId()).capabilityCode(item.getCapabilityCode())
                .version(item.getVersion()).name(item.getName()).module(item.getModule()).description(item.getDescription())
                .inputSchemaJson(item.getInputSchemaJson()).outputSchemaJson(item.getOutputSchemaJson())
                .readOnly(item.isReadOnly()).requiresConfirmation(item.isRequiresConfirmation())
                .tenantScope(item.getTenantScope()).source(item.getSource()).sourceUrl(item.getSourceUrl())
                .freshness(item.getFreshness()).availability(item.getAvailability()).enabled(item.isEnabled())
                .governanceAvailability(item.getGovernanceAvailability())
                .priority(item.getPriority()).promptKey(item.getPromptKey()).runtimeEntry(item.getRuntimeEntry())
                .routeGroup(item.getRouteGroup()).lastSeenAt(item.getLastSeenAt() == null ? null : item.getLastSeenAt().toString())
                .staleAt(item.getStaleAt() == null ? null : item.getStaleAt().toString()).build();
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
