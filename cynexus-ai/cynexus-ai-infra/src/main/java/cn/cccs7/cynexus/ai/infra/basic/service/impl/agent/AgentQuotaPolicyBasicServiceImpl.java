package cn.cccs7.cynexus.ai.infra.basic.service.impl.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentQuotaPolicyPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentQuotaPolicyMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentQuotaPolicyBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
import org.springframework.stereotype.Service;

@Service
public class AgentQuotaPolicyBasicServiceImpl extends AbstractCrudBasicService<AgentQuotaPolicyPO>
        implements AgentQuotaPolicyBasicService {
    private final AgentQuotaPolicyMapper mapper;

    public AgentQuotaPolicyBasicServiceImpl(AgentQuotaPolicyMapper mapper, SnowflakeIdGenerator idGenerator) {
        super(mapper, idGenerator);
        this.mapper = mapper;
    }

    @Override
    public AgentQuotaPolicyPO findApplicable(String tenantKey, String principalKey) {
        return mapper.findApplicable(tenantKey, principalKey);
    }
}
