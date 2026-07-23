package cn.cccs7.cynexus.ai.application.service.agent.write;

import cn.cccs7.cynexus.ai.api.dto.AgentWritePreviewDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentWriteResultDTO;
import cn.cccs7.cynexus.ai.api.request.AgentWriteConfirmRequest;
import cn.cccs7.cynexus.ai.api.request.AgentWritePreviewRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.service.agent.write.AgentWriteDomainService;
import org.springframework.stereotype.Service;

@Service
public class AgentWriteAppService {
    private final AgentWriteDomainService domainService;

    public AgentWriteAppService(AgentWriteDomainService domainService) {
        this.domainService = domainService;
    }

    public AgentWritePreviewDTO preview(AgentIdentity identity, AgentWritePreviewRequest request) {
        String toolCode = request == null ? null : request.getToolCode();
        String argumentsJson = request == null ? null : request.getArgumentsJson();
        var preview = domainService.preview(identity, toolCode, argumentsJson,
                request == null ? null : request.getConversationNo(), request == null ? null : request.getRunNo());
        var command = preview.getCommand();
        return AgentWritePreviewDTO.builder().commandNo(command.getCommandNo()).toolCode(command.getToolCode())
                .argumentDigest(command.getArgumentDigest()).argumentsJson(command.getArgumentsJson()).confirmationToken(preview.getConfirmationToken())
                .expiresAt(command.getExpiresAt()).status(command.getStatus()).build();
    }

    public AgentWriteResultDTO confirm(AgentIdentity identity, String commandNo, AgentWriteConfirmRequest request) {
        var confirmed = domainService.confirm(identity, commandNo,
                request == null ? null : request.getConfirmationToken());
        return AgentWriteResultDTO.builder().commandNo(commandNo).status(confirmed.getStatus())
                .resultJson(confirmed.getResultJson()).errorMessage(confirmed.getErrorMessage()).build();
    }

    public AgentWriteResultDTO reject(AgentIdentity identity, String commandNo) {
        var command = domainService.reject(identity, commandNo);
        return AgentWriteResultDTO.builder().commandNo(commandNo).status(command.getStatus()).build();
    }
}
