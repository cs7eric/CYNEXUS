package cn.cccs7.cynexus.ai.application.job.agent;

import cn.cccs7.cynexus.ai.domain.service.agent.maintenance.AgentRunRecoveryDomainService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reconciles terminal state after a process crash or an expired distributed lease. */
@Component
public class AgentStaleRunRecoveryJob {

    private final AgentRunRecoveryDomainService recoveryDomainService;

    public AgentStaleRunRecoveryJob(AgentRunRecoveryDomainService recoveryDomainService) {
        this.recoveryDomainService = recoveryDomainService;
    }

    @Scheduled(
            initialDelayString = "${cynexus.ai.agent.recovery-initial-delay-ms:30000}",
            fixedDelayString = "${cynexus.ai.agent.recovery-delay-ms:60000}")
    public void recover() {
        recoveryDomainService.recoverStaleRuns();
    }
}
