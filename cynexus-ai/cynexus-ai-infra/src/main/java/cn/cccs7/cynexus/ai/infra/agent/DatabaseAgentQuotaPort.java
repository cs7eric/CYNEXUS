package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentQuotaPort;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentUsageDailyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAgentQuotaPort implements AgentQuotaPort {
    private final AgentUsageDailyMapper mapper;
    private final int maxRequests;
    private final int maxTokens;

    public DatabaseAgentQuotaPort(AgentUsageDailyMapper mapper,
                                  @Value("${cynexus.ai.agent.quota.daily-requests:200}") int maxRequests,
                                  @Value("${cynexus.ai.agent.quota.daily-tokens:100000}") int maxTokens) {
        this.mapper = mapper;
        this.maxRequests = Math.max(1, maxRequests);
        this.maxTokens = Math.max(1_024, maxTokens);
    }

    @Override
    public boolean reserveRequest(String ownerKey, String tenantKey, String principalKey, int estimatedTokens) {
        return mapper.reserve(ownerKey, LocalDate.now(ZoneOffset.UTC), value(tenantKey), value(principalKey),
                Math.max(1, estimatedTokens), maxRequests, maxTokens) > 0;
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
