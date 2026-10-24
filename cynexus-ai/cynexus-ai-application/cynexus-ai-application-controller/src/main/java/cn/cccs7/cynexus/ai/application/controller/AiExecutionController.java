package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.application.service.ScenarioExecutionAppService;
import cn.cccs7.cynexus.ai.application.service.AiVisionTaskAppService;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskEnqueueDTO;
import cn.cccs7.cynexus.ai.api.dto.AiVisionTaskSummaryDTO;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/ai/executions")
public class AiExecutionController {

    private final ScenarioExecutionAppService scenarioExecutionAppService;
    private final AgentIdentityResolver identityResolver;
    private final AiVisionTaskAppService visionTaskAppService;

    public AiExecutionController(ScenarioExecutionAppService scenarioExecutionAppService, AgentIdentityResolver identityResolver) {
        this(scenarioExecutionAppService, identityResolver, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiExecutionController(ScenarioExecutionAppService scenarioExecutionAppService,
                                 AgentIdentityResolver identityResolver,
                                 AiVisionTaskAppService visionTaskAppService) {
        this.scenarioExecutionAppService = scenarioExecutionAppService;
        this.identityResolver = identityResolver;
        this.visionTaskAppService = visionTaskAppService;
    }

    @PostMapping
    public Result<AiExecuteDTO> execute(@RequestBody AiExecuteRequest request,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        var identity = requiresTrustedIdentity(request == null ? null : request.getScenarioCode())
                ? resolveRequired(principal, tenant, signature)
                : resolveOptional(principal, tenant, signature);
        return Result.success(scenarioExecutionAppService.execute(request,
                identity == null ? null : identity.getTenantKey(), identity == null ? null : identity.getPrincipalKey()));
    }

    @PostMapping("/vision")
    public Result<AiExecuteDTO> executeVision(@RequestBody AiVisionExecuteRequest request,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        var identity = requiresTrustedIdentity(request == null ? null : request.getScenarioCode())
                ? resolveRequired(principal, tenant, signature)
                : resolveOptional(principal, tenant, signature);
        return Result.success(scenarioExecutionAppService.executeVision(request,
                identity == null ? null : identity.getTenantKey(), identity == null ? null : identity.getPrincipalKey()));
    }

    @PostMapping("/vision/tasks")
    public ResponseEntity<Result<AiVisionTaskEnqueueDTO>> enqueueVision(
            @RequestBody AiVisionExecuteRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        requireTaskService();
        var identity = resolveRequired(principal, tenant, signature);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(visionTaskAppService.enqueue(
                request, identity.getTenantKey(), identity.getPrincipalKey(), idempotencyKey)));
    }

    @GetMapping("/vision/tasks/{id}")
    public Result<AiVisionTaskDTO> getVisionTask(
            @PathVariable Long id,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        requireTaskService();
        var identity = resolveRequired(principal, tenant, signature);
        AiVisionTaskDTO task = visionTaskAppService.get(id, identity.getTenantKey(), identity.getPrincipalKey());
        if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vision task not found");
        return Result.success(task);
    }

    @GetMapping("/vision/tasks")
    public Result<List<AiVisionTaskSummaryDTO>> listVisionTasks(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        requireTaskService();
        var identity = resolveRequired(principal, tenant, signature);
        return Result.success(visionTaskAppService.list(limit, identity.getTenantKey(), identity.getPrincipalKey()));
    }

    @PostMapping("/vision/tasks/{id}/retry")
    public Result<AiVisionTaskSummaryDTO> retryVisionTask(
            @PathVariable Long id,
            @RequestHeader(value = "X-Cynexus-Tenant", required = false) String tenant,
            @RequestHeader(value = "X-Cynexus-Principal", required = false) String principal,
            @RequestHeader(value = "X-Cynexus-Identity-Signature", required = false) String signature) {
        requireTaskService();
        var identity = resolveRequired(principal, tenant, signature);
        try {
            AiVisionTaskSummaryDTO task = visionTaskAppService.retry(id, identity.getTenantKey(), identity.getPrincipalKey());
            if (task == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vision task not found");
            return Result.success(task);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    private void requireTaskService() {
        if (visionTaskAppService == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Vision task queue is unavailable");
    }

    private cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity resolveOptional(String principal, String tenant, String signature) {
        if ((principal == null || principal.isBlank()) && (tenant == null || tenant.isBlank())
                && (signature == null || signature.isBlank())) return null;
        return identityResolver.resolve(principal, tenant, signature);
    }

    private boolean requiresTrustedIdentity(String scenarioCode) {
        return "food_fridge_inventory".equals(scenarioCode)
                || "food_nutrition_label_parse".equals(scenarioCode);
    }

    private AgentIdentity resolveRequired(String principal, String tenant, String signature) {
        if (principal == null || principal.isBlank() || tenant == null || tenant.isBlank()
                || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A trusted identity is required for this food recognition request");
        }
        try {
            return identityResolver.resolve(principal, tenant, signature);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception);
        }
    }
}
