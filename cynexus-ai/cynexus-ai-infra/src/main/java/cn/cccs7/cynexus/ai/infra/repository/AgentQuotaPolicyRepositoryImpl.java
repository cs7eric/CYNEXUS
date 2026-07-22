package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentQuotaPolicyBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AgentQuotaPolicyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AgentQuotaPolicyRepositoryImpl implements AgentQuotaPolicyRepository {
    private final AgentQuotaPolicyBasicService service;

    public AgentQuotaPolicyRepositoryImpl(AgentQuotaPolicyBasicService service) { this.service = service; }

    @Override public List<AgentQuotaPolicyBO> listAll() { return service.listAll().stream().map(AgentQuotaPolicyPOConverter.INSTANCE::toTarget).toList(); }
    @Override public AgentQuotaPolicyBO findById(Long id) { return Optional.ofNullable(service.getById(id)).map(AgentQuotaPolicyPOConverter.INSTANCE::toTarget).orElse(null); }
    @Override public AgentQuotaPolicyBO findApplicable(String tenantKey, String principalKey) {
        return Optional.ofNullable(service.findApplicable(tenantKey, principalKey)).map(AgentQuotaPolicyPOConverter.INSTANCE::toTarget).orElse(null);
    }
    @Override public Long create(AgentQuotaPolicyBO policy) { return service.create(AgentQuotaPolicyPOConverter.INSTANCE.toSource(policy)); }
    @Override public boolean update(AgentQuotaPolicyBO policy) { return service.update(AgentQuotaPolicyPOConverter.INSTANCE.toSource(policy)); }
    @Override public boolean delete(Long id) { return service.delete(id); }
}
