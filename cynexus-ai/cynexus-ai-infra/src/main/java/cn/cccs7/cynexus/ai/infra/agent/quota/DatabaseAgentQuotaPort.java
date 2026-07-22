package cn.cccs7.cynexus.ai.infra.agent.quota;

import cn.cccs7.cynexus.ai.domain.gateway.quota.AgentQuotaPort;
import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.infra.basic.mapper.usage.AgentUsageDailyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAgentQuotaPort implements AgentQuotaPort {
    private final AgentUsageDailyMapper mapper;
    private final AgentQuotaPolicyRepository policyRepository;

    public DatabaseAgentQuotaPort(AgentUsageDailyMapper mapper, AgentQuotaPolicyRepository policyRepository) {
        this.mapper = mapper;
        this.policyRepository = policyRepository;
    }

    @Override
    public boolean reserveRequest(String ownerKey, String tenantKey, String principalKey, int estimatedTokens) {
        AgentQuotaPolicyBO policy = policyRepository.findApplicable(value(tenantKey), value(principalKey));
        if (policy == null) return false;
        return mapper.reserve(ownerKey, LocalDate.now(ZoneOffset.UTC), value(tenantKey), value(principalKey),
                Math.max(1, estimatedTokens), policy.getDailyRequestLimit(), Math.toIntExact(policy.getDailyTokenLimit())) > 0;
    }

    @Override
    public void recordUsage(String ownerKey, String tenantKey, String principalKey, int reservedTokens,
                            int inputTokens, int outputTokens, int totalTokens, BigDecimal estimatedCost, String usageSource) {
        mapper.record(ownerKey, LocalDate.now(ZoneOffset.UTC), Math.max(0, reservedTokens), Math.max(0, inputTokens),
                Math.max(0, outputTokens), Math.max(0, totalTokens), estimatedCost == null ? BigDecimal.ZERO : estimatedCost,
                "REPORTED".equals(usageSource) ? 0 : 1);
    }

    private String value(String value) { return value == null ? "" : value; }
}
