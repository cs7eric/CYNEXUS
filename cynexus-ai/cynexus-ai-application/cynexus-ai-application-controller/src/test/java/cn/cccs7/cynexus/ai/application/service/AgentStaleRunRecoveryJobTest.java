package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.service.agent.maintenance.AgentRunRecoveryDomainService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentStaleRunRecoveryJobTest {

    @Test
    void onlyRecoversRunsWithoutAnActiveDistributedLease() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        AgentRuntimeLeasePort lease = mock(AgentRuntimeLeasePort.class);
        AgentRunBO stale = AgentRunBO.builder().runNo("AGR-stale").status("RUNNING").build();
        when(repository.findStaleRunningRuns(any())).thenReturn(List.of(stale));
        when(lease.isLeaseActive("AGR-stale")).thenReturn(false);

        AgentRunRecoveryDomainService service = new AgentRunRecoveryDomainService(
                repository, lease, Clock.fixed(Instant.parse("2026-07-23T00:00:00Z"), ZoneOffset.UTC), 15);
        assertEquals(1, service.recoverStaleRuns(OffsetDateTime.parse("2026-07-23T00:00:00Z")));

        assertEquals("FAILED", stale.getStatus());
        assertEquals("STALE_RECOVERED", stale.getFailureCategory());
        verify(repository).updateRun(stale);
    }
}
