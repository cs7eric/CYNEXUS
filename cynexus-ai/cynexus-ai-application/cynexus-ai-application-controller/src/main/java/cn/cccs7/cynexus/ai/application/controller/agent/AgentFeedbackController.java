package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentFeedbackSummaryDTO;
import cn.cccs7.cynexus.ai.api.request.AgentFeedbackRequest;
import cn.cccs7.cynexus.ai.application.service.AgentFeedbackAppService;
import cn.cccs7.cynexus.ai.application.service.AgentIdentityResolver;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/agent")
public class AgentFeedbackController {
    private final AgentFeedbackAppService appService;
    private final AgentIdentityResolver identityResolver;

    public AgentFeedbackController(AgentFeedbackAppService appService, AgentIdentityResolver identityResolver) {
        this.appService = appService;
        this.identityResolver = identityResolver;
    }

    @PostMapping("/runs/{runNo}/feedback")
    public Result<AgentFeedbackDTO> submit(@PathVariable String runNo, @RequestBody AgentFeedbackRequest request,
                                           HttpServletRequest httpRequest) {
        return Result.success(appService.submit(identity(httpRequest), runNo, request));
    }

    @GetMapping("/runs/{runNo}/feedback")
    public Result<AgentFeedbackDTO> get(@PathVariable String runNo, HttpServletRequest httpRequest) {
        return Result.success(appService.get(identity(httpRequest), runNo));
    }

    @GetMapping("/feedback/summary")
    public Result<AgentFeedbackSummaryDTO> summary(HttpServletRequest httpRequest) {
        return Result.success(appService.summary(identity(httpRequest)));
    }

    private AgentIdentity identity(HttpServletRequest request) {
        try {
            return identityResolver.resolve(request.getHeader("X-Cynexus-Principal"), request.getHeader("X-Cynexus-Tenant"),
                    request.getHeader("X-Cynexus-Identity-Signature"));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception);
        }
    }
}
