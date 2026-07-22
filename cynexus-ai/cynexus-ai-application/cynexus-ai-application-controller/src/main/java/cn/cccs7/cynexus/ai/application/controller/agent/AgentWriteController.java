package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AgentWritePreviewDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentWriteResultDTO;
import cn.cccs7.cynexus.ai.api.request.AgentWriteConfirmRequest;
import cn.cccs7.cynexus.ai.api.request.AgentWritePreviewRequest;
import cn.cccs7.cynexus.ai.application.service.AgentIdentityResolver;
import cn.cccs7.cynexus.ai.application.service.AgentWriteAppService;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/agent/write-commands")
public class AgentWriteController {
    private final AgentWriteAppService appService;
    private final AgentIdentityResolver identityResolver;

    public AgentWriteController(AgentWriteAppService appService, AgentIdentityResolver identityResolver) {
        this.appService = appService;
        this.identityResolver = identityResolver;
    }

    @PostMapping("/preview")
    public Result<AgentWritePreviewDTO> preview(@RequestBody AgentWritePreviewRequest request, HttpServletRequest httpRequest) {
        return Result.success(appService.preview(identity(httpRequest), request));
    }

    @PostMapping("/{commandNo}/confirm")
    public Result<AgentWriteResultDTO> confirm(@PathVariable String commandNo, @RequestBody AgentWriteConfirmRequest request,
                                              HttpServletRequest httpRequest) {
        return Result.success(appService.confirm(identity(httpRequest), commandNo, request));
    }

    @PostMapping("/{commandNo}/reject")
    public Result<AgentWriteResultDTO> reject(@PathVariable String commandNo, HttpServletRequest httpRequest) {
        return Result.success(appService.reject(identity(httpRequest), commandNo));
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
