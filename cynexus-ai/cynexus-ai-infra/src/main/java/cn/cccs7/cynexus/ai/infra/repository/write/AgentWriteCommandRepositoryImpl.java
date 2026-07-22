package cn.cccs7.cynexus.ai.infra.repository.write;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentWriteCommandRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.write.AgentWriteCommandBasicService;
import cn.cccs7.cynexus.ai.infra.convert.write.AgentWriteCommandPOConverter;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AgentWriteCommandRepositoryImpl implements AgentWriteCommandRepository {
    private final AgentWriteCommandBasicService service;
    public AgentWriteCommandRepositoryImpl(AgentWriteCommandBasicService service) { this.service = service; }
    @Override public AgentWriteCommandBO save(AgentWriteCommandBO command) { return AgentWriteCommandPOConverter.INSTANCE.toTarget(service.save(AgentWriteCommandPOConverter.INSTANCE.toSource(command))); }
    @Override public Optional<AgentWriteCommandBO> findPending(String ownerKey, String commandNo) { return Optional.ofNullable(service.findPending(ownerKey, commandNo)).map(AgentWriteCommandPOConverter.INSTANCE::toTarget); }
    @Override public Optional<AgentWriteCommandBO> markConfirmed(String ownerKey, String commandNo, String tokenHash) { return Optional.ofNullable(service.markConfirmed(ownerKey, commandNo, tokenHash, OffsetDateTime.now())).map(AgentWriteCommandPOConverter.INSTANCE::toTarget); }
    @Override public void markExecuted(AgentWriteCommandBO command) { service.markExecuted(AgentWriteCommandPOConverter.INSTANCE.toSource(command)); }
    @Override public void markRejected(AgentWriteCommandBO command) { service.markRejected(AgentWriteCommandPOConverter.INSTANCE.toSource(command)); }
}
