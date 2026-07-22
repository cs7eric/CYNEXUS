package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AgentWritePreviewDTO;
import cn.cccs7.cynexus.ai.api.dto.AgentWriteResultDTO;
import cn.cccs7.cynexus.ai.api.request.AgentWriteConfirmRequest;
import cn.cccs7.cynexus.ai.api.request.AgentWritePreviewRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentWriteExecutorPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentWritePolicy;
import cn.cccs7.cynexus.ai.domain.agent.AgentWriteResult;
import cn.cccs7.cynexus.ai.domain.repository.AgentWriteCommandRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentWriteAppService {
    private final AgentWriteCommandRepository repository;
    private final AgentWriteExecutorPort executor;
    private final ObjectMapper objectMapper;

    public AgentWriteAppService(AgentWriteCommandRepository repository, AgentWriteExecutorPort executor, ObjectMapper objectMapper) {
        this.repository = repository;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    public AgentWritePreviewDTO preview(AgentIdentity identity, AgentWritePreviewRequest request) {
        AgentIdentity safe = identity == null ? AgentIdentity.singleUser() : identity;
        String toolCode = request == null ? null : request.getToolCode();
        String argumentsJson = request == null ? null : request.getArgumentsJson();
        String digest = AgentWritePolicy.validateAndDigest(toolCode, argumentsJson);
        Map<String, Object> arguments = parse(argumentsJson);
        AgentWritePolicy.validateArguments(arguments);
        String token = AgentWritePolicy.issueToken();
        AgentWriteCommandBO command = repository.save(AgentWriteCommandBO.builder()
                .commandNo("AWC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .ownerKey(safe.storageOwnerKey()).tenantKey(safe.getTenantKey()).principalKey(safe.getPrincipalKey())
                .conversationNo(request.getConversationNo()).runNo(request.getRunNo()).toolCode(toolCode)
                .argumentsJson(argumentsJson.trim()).argumentDigest(digest).confirmationTokenHash(AgentWritePolicy.hashToken(token))
                .status("PENDING_CONFIRMATION").expiresAt(AgentWritePolicy.expiresAt()).build());
        return AgentWritePreviewDTO.builder().commandNo(command.getCommandNo()).toolCode(command.getToolCode())
                .argumentDigest(command.getArgumentDigest()).argumentsJson(command.getArgumentsJson()).confirmationToken(token)
                .expiresAt(command.getExpiresAt()).status(command.getStatus()).build();
    }

    public AgentWriteResultDTO confirm(AgentIdentity identity, String commandNo, AgentWriteConfirmRequest request) {
        String owner = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        String tokenHash = AgentWritePolicy.hashToken(request == null ? null : request.getConfirmationToken());
        AgentWriteCommandBO pending = repository.findPending(owner, commandNo).orElseThrow(() -> new IllegalArgumentException("Write command is not pending"));
        if (pending.getExpiresAt() == null || pending.getExpiresAt().isBefore(OffsetDateTime.now())) throw new IllegalStateException("Write confirmation has expired");
        AgentWriteCommandBO confirmed = repository.markConfirmed(owner, commandNo, tokenHash)
                .orElseThrow(() -> new IllegalStateException("Write command was already confirmed, rejected, or token did not match"));
        AgentWriteResult result = executor.execute(confirmed);
        confirmed.setResultJson(result.getResultJson());
        confirmed.setErrorMessage(result.getErrorMessage());
        confirmed.setStatus(result.isSuccess() ? "EXECUTED" : "FAILED");
        confirmed.setExecutedAt(OffsetDateTime.now());
        repository.markExecuted(confirmed);
        return AgentWriteResultDTO.builder().commandNo(commandNo).status(confirmed.getStatus())
                .resultJson(confirmed.getResultJson()).errorMessage(confirmed.getErrorMessage()).build();
    }

    public AgentWriteResultDTO reject(AgentIdentity identity, String commandNo) {
        String owner = identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
        AgentWriteCommandBO command = repository.findPending(owner, commandNo).orElseThrow(() -> new IllegalArgumentException("Write command is not pending"));
        command.setStatus("REJECTED");
        repository.markRejected(command);
        return AgentWriteResultDTO.builder().commandNo(commandNo).status("REJECTED").build();
    }

    private Map<String, Object> parse(String value) {
        try { return objectMapper.readValue(value, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalArgumentException("Write arguments must be valid JSON", exception); }
    }
}
