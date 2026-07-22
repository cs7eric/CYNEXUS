package cn.cccs7.cynexus.ai.infra.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.infra.basic.mapper.usage.AgentUsageDailyMapper;
import cn.cccs7.cynexus.ai.infra.agent.quota.DatabaseAgentQuotaPort;
import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RedisBackedAgentQuotaPortTest {
    @Test
    void returnsFalseWhenDurableReservationIsRejectedAndRecordsActualUsage() {
        AgentUsageDailyMapper mapper = mock(AgentUsageDailyMapper.class);
        when(mapper.reserve(anyString(), any(), anyString(), anyString(), anyInt(), anyInt(), anyInt())).thenReturn(0);
        AgentQuotaPolicyRepository policies = mock(AgentQuotaPolicyRepository.class);
        when(policies.findApplicable("tenant", "user")).thenReturn(AgentQuotaPolicyBO.builder()
                .dailyRequestLimit(10).dailyTokenLimit(1000L).build());
        DatabaseAgentQuotaPort quota = new DatabaseAgentQuotaPort(mapper, policies);

        assertThat(quota.reserveRequest("tenant::user", "tenant", "user", 100)).isFalse();
        quota.recordUsage("tenant::user", "tenant", "user", 100, 20, 30, 50, BigDecimal.ONE, "ESTIMATED");

        verify(mapper).record(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt());
    }

    @Test
    void rejectsRequestsWithoutAnApplicableDatabasePolicy() {
        AgentUsageDailyMapper mapper = mock(AgentUsageDailyMapper.class);
        AgentQuotaPolicyRepository policies = mock(AgentQuotaPolicyRepository.class);
        when(policies.findApplicable("tenant", "user")).thenReturn(null);
        DatabaseAgentQuotaPort quota = new DatabaseAgentQuotaPort(mapper, policies);

        assertThat(quota.reserveRequest("tenant::user", "tenant", "user", 100)).isFalse();
        verify(mapper, org.mockito.Mockito.never()).reserve(anyString(), any(), anyString(), anyString(), anyInt(), anyInt(), anyInt());
    }
}
