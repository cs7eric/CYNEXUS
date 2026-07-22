package cn.cccs7.cynexus.ai.application.controller.agent;

import cn.cccs7.cynexus.ai.api.dto.AgentTaskDTO;
import cn.cccs7.cynexus.ai.api.request.AgentTaskRequest;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import cn.cccs7.cynexus.ai.application.service.agent.task.AgentTaskAppService;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/agent/tasks")
public class AgentTaskController {
    private final AgentTaskAppService appService;
    private final AgentIdentityResolver identityResolver;
    public AgentTaskController(AgentTaskAppService appService, AgentIdentityResolver identityResolver) { this.appService = appService; this.identityResolver = identityResolver; }
    @PostMapping public Result<AgentTaskDTO> schedule(@RequestBody AgentTaskRequest request, HttpServletRequest httpRequest) { return Result.success(appService.schedule(identity(httpRequest), request)); }
    @GetMapping public Result<List<AgentTaskDTO>> list(@RequestParam(defaultValue = "30") int limit, HttpServletRequest httpRequest) { return Result.success(appService.list(identity(httpRequest), limit)); }
    @GetMapping("/{taskNo}") public Result<AgentTaskDTO> get(@PathVariable String taskNo, HttpServletRequest httpRequest) { return Result.success(appService.find(identity(httpRequest), taskNo)); }
    @PostMapping("/{taskNo}/pause") public Result<Boolean> pause(@PathVariable String taskNo, HttpServletRequest httpRequest) { return Result.success(appService.pause(identity(httpRequest), taskNo)); }
    @PostMapping("/{taskNo}/resume") public Result<Boolean> resume(@PathVariable String taskNo, HttpServletRequest httpRequest) { return Result.success(appService.resume(identity(httpRequest), taskNo)); }
    @PostMapping("/{taskNo}/cancel") public Result<Boolean> cancel(@PathVariable String taskNo, HttpServletRequest httpRequest) { return Result.success(appService.cancel(identity(httpRequest), taskNo)); }
    private AgentIdentity identity(HttpServletRequest request) { try { return identityResolver.resolve(request.getHeader("X-Cynexus-Principal"), request.getHeader("X-Cynexus-Tenant"), request.getHeader("X-Cynexus-Identity-Signature")); } catch (IllegalStateException exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception); } }
}
