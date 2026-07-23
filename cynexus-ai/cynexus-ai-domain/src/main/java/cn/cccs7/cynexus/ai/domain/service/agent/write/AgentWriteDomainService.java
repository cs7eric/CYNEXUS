package cn.cccs7.cynexus.ai.domain.service.agent.write;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentWriteExecutorPort;
import cn.cccs7.cynexus.ai.domain.policy.capability.AgentWritePolicy;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentWriteCommandRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentIdentity;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWritePreview;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentWriteResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Owns the confirmed-write lifecycle and its argument/token invariants. */
@Service
public class AgentWriteDomainService {
    private final AgentWriteCommandRepository repository;
    private final AgentWriteExecutorPort executor;
    private final ObjectMapper objectMapper;

    public AgentWriteDomainService(AgentWriteCommandRepository repository,
                                   AgentWriteExecutorPort executor,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    public AgentWritePreview preview(AgentIdentity identity, String toolCode, String argumentsJson,
                                     String conversationNo, String runNo) {
        AgentIdentity safe = identity == null ? AgentIdentity.singleUser() : identity;
        String digest = AgentWritePolicy.validateAndDigest(toolCode, argumentsJson);
        Map<String, Object> arguments = parse(argumentsJson);
        AgentWritePolicy.validateArguments(arguments);
        String token = AgentWritePolicy.issueToken();
        AgentWriteCommandBO command = repository.save(AgentWriteCommandBO.builder()
                .commandNo("AWC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .ownerKey(safe.storageOwnerKey()).tenantKey(safe.getTenantKey()).principalKey(safe.getPrincipalKey())
                .conversationNo(conversationNo).runNo(runNo).toolCode(toolCode)
                .argumentsJson(argumentsJson.trim()).argumentDigest(digest)
                .confirmationTokenHash(AgentWritePolicy.hashToken(token)).status("PENDING_CONFIRMATION")
                .expiresAt(AgentWritePolicy.expiresAt()).build());
        return AgentWritePreview.builder().command(command).confirmationToken(token).build();
    }

    public AgentWriteCommandBO confirm(AgentIdentity identity, String commandNo, String confirmationToken) {
        String owner = owner(identity);
        AgentWriteCommandBO pending = repository.findPending(owner, commandNo)
                .orElseThrow(() -> new IllegalArgumentException("Write command is not pending"));
        if (pending.getExpiresAt() == null || pending.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Write confirmation has expired");
        }
        AgentWriteCommandBO confirmed = repository.markConfirmed(owner, commandNo,
                        AgentWritePolicy.hashToken(confirmationToken))
                .orElseThrow(() -> new IllegalStateException(
                        "Write command was already confirmed, rejected, or token did not match"));
        AgentWriteResult result = executor.execute(confirmed);
        confirmed.setResultJson(result.getResultJson());
        confirmed.setErrorMessage(result.getErrorMessage());
        confirmed.setStatus(result.isSuccess() ? "EXECUTED" : "FAILED");
        confirmed.setExecutedAt(OffsetDateTime.now());
        repository.markExecuted(confirmed);
        return confirmed;
    }

    public AgentWriteCommandBO reject(AgentIdentity identity, String commandNo) {
        AgentWriteCommandBO command = repository.findPending(owner(identity), commandNo)
                .orElseThrow(() -> new IllegalArgumentException("Write command is not pending"));
        command.setStatus("REJECTED");
        repository.markRejected(command);
        return command;
    }

    private String owner(AgentIdentity identity) {
        return identity == null ? AgentIdentity.singleUser().storageOwnerKey() : identity.storageOwnerKey();
    }

    private Map<String, Object> parse(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Write arguments must be valid JSON", exception);
        }
    }
}
