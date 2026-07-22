package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import java.util.List;

public interface AgentQuotaPolicyRepository {
    List<AgentQuotaPolicyBO> listAll();
    AgentQuotaPolicyBO findById(Long id);
    AgentQuotaPolicyBO findApplicable(String tenantKey, String principalKey);
    Long create(AgentQuotaPolicyBO policy);
    boolean update(AgentQuotaPolicyBO policy);
    boolean delete(Long id);
}
