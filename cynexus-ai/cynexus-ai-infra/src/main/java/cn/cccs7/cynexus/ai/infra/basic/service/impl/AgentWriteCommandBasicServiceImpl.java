package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentWriteCommandPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentWriteAuditMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentWriteCommandMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentWriteCommandBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AgentWriteCommandBasicServiceImpl implements AgentWriteCommandBasicService {
    private final AgentWriteCommandMapper mapper;
    private final AgentWriteAuditMapper auditMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentWriteCommandBasicServiceImpl(AgentWriteCommandMapper mapper, AgentWriteAuditMapper auditMapper,
                                             SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.auditMapper = auditMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public AgentWriteCommandPO save(AgentWriteCommandPO command) {
        if (command.getId() == null) command.setId(idGenerator.nextId());
        command.setCreatedAt(OffsetDateTime.now());
        mapper.insert(command);
        auditMapper.insert(command.getCommandNo(), command.getOwnerKey(), "PREVIEW", command.getStatus(), command.getToolCode(),
                command.getArgumentDigest(), null, null, OffsetDateTime.now());
        return command;
    }

    @Override public AgentWriteCommandPO findPending(String ownerKey, String commandNo) { return mapper.findPending(ownerKey, commandNo); }

    @Override
    public AgentWriteCommandPO markConfirmed(String ownerKey, String commandNo, String tokenHash, OffsetDateTime now) {
        if (mapper.markConfirmed(ownerKey, commandNo, tokenHash, now) == 0) return null;
        AgentWriteCommandPO command = mapper.findPending(ownerKey, commandNo);
        if (command == null) {
            command = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentWriteCommandPO>()
                    .eq("owner_key", ownerKey).eq("command_no", commandNo));
        }
        auditMapper.insert(commandNo, ownerKey, "CONFIRM", "CONFIRMED", command == null ? null : command.getToolCode(),
                command == null ? null : command.getArgumentDigest(), null, null, OffsetDateTime.now());
        return command;
    }

    @Override
    public void markExecuted(AgentWriteCommandPO command) {
        mapper.markExecuted(command);
        auditMapper.insert(command.getCommandNo(), command.getOwnerKey(), "EXECUTE", command.getStatus(), command.getToolCode(),
                command.getArgumentDigest(), command.getResultJson(), command.getErrorMessage(), OffsetDateTime.now());
    }

    @Override
    public void markRejected(AgentWriteCommandPO command) {
        mapper.markRejected(command);
        auditMapper.insert(command.getCommandNo(), command.getOwnerKey(), "REJECT", "REJECTED", command.getToolCode(),
                command.getArgumentDigest(), null, null, OffsetDateTime.now());
    }
}
