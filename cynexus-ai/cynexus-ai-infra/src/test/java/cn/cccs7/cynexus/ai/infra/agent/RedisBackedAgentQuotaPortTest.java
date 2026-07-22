package cn.cccs7.cynexus.ai.infra.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentUsageDailyMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RedisBackedAgentQuotaPortTest {
    @Test
    void returnsFalseWhenDurableReservationIsRejectedAndRecordsActualUsage() {
        AgentUsageDailyMapper mapper = mock(AgentUsageDailyMapper.class);
        when(mapper.reserve(anyString(), any(), anyString(), anyString(), anyInt(), anyInt(), anyInt())).thenReturn(0);
        DatabaseAgentQuotaPort quota = new DatabaseAgentQuotaPort(mapper, 10, 1000);

        assertThat(quota.reserveRequest("tenant::user", "tenant", "user", 100)).isFalse();
        quota.recordUsage("tenant::user", "tenant", "user", 100, 20, 30, 50, BigDecimal.ONE, "ESTIMATED");

        verify(mapper).record(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt());
    }
}
