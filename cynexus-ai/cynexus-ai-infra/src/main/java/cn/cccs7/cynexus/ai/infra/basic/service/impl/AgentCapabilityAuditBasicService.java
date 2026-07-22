package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentCapabilityAuditPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentCapabilityAuditMapper;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

/** Dedicated persistence service kept separate from capability state CRUD. */
@Service
public class AgentCapabilityAuditBasicService {
    private final AgentCapabilityAuditMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentCapabilityAuditBasicService(AgentCapabilityAuditMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    public AgentCapabilityAuditPO save(AgentCapabilityAuditPO audit) {
        if (audit.getId() == null) audit.setId(idGenerator.nextId());
        if (audit.getOccurredAt() == null) audit.setOccurredAt(OffsetDateTime.now());
        audit.setCreatedAt(audit.getOccurredAt());
        audit.setUpdatedAt(audit.getOccurredAt());
        mapper.insert(audit);
        return audit;
    }
}
