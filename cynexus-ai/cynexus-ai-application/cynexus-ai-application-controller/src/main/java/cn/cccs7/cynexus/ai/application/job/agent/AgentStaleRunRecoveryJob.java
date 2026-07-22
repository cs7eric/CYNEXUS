package cn.cccs7.cynexus.ai.application.job.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reconciles terminal state after a process crash or an expired distributed lease. */
@Component
public class AgentStaleRunRecoveryJob {

    private final AgentRunRepository runRepository;
    private final AgentRuntimeLeasePort runtimeLease;

    public AgentStaleRunRecoveryJob(AgentRunRepository runRepository, AgentRuntimeLeasePort runtimeLease) {
        this.runRepository = runRepository;
        this.runtimeLease = runtimeLease;
    }

    @Scheduled(
            initialDelayString = "${cynexus.ai.agent.recovery-initial-delay-ms:30000}",
            fixedDelayString = "${cynexus.ai.agent.recovery-delay-ms:60000}")
    public void recover() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(15);
        List<AgentRunBO> candidates = runRepository.findStaleRunningRuns(cutoff);
        for (AgentRunBO run : candidates) {
            if (run == null || run.getRunNo() == null || runtimeLease.isLeaseActive(run.getRunNo())) continue;
            run.setStatus("FAILED");
            run.setFailureCategory("STALE_RECOVERED");
            run.setErrorMessage("Agent run was recovered after its distributed lease expired");
            run.setFinishedAt(OffsetDateTime.now());
            runRepository.updateRun(run);
        }
    }
}
