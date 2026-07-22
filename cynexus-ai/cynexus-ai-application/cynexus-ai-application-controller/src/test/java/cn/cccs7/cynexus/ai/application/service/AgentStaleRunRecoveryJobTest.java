package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.job.agent.AgentStaleRunRecoveryJob;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
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

        new AgentStaleRunRecoveryJob(repository, lease).recover();

        assertEquals("FAILED", stale.getStatus());
        assertEquals("STALE_RECOVERED", stale.getFailureCategory());
        verify(repository).updateRun(stale);
    }
}
