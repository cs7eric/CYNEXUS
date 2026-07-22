package cn.cccs7.cynexus.ai.infra.basic.service.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentQuotaPolicyPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;

public interface AgentQuotaPolicyBasicService extends CrudBasicService<AgentQuotaPolicyPO> {
    AgentQuotaPolicyPO findApplicable(String tenantKey, String principalKey);
}
