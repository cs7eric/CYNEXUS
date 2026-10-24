package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.api.request.AiVisionExecuteRequest;
import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.application.service.ScenarioExecutionAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import cn.cccs7.cynexus.ai.application.service.agent.identity.AgentIdentityResolver;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/executions")
public class AiExecutionController {

    private final ScenarioExecutionAppService scenarioExecutionAppService;
    private final AgentIdentityResolver identityResolver;

    public AiExecutionController(ScenarioExecutionAppService scenarioExecutionAppService, AgentIdentityResolver identityResolver) {
        this.scenarioExecutionAppService = scenarioExecutionAppService;
        this.identityResolver = identityResolver;
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
