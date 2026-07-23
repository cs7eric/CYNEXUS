package cn.cccs7.cynexus.ai.domain.service.agent.maintenance;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/** Owns stale-run detection and the terminal state transition after lease loss. */
public final class AgentRunRecoveryDomainService {
    private static final int DEFAULT_STALE_MINUTES = 15;
    private static final String RECOVERY_ERROR =
            "Agent run was recovered after its distributed lease expired";

    private final AgentRunRepository runRepository;
    private final AgentRuntimeLeasePort runtimeLease;
    private final Clock clock;
    private final int staleMinutes;

    public AgentRunRecoveryDomainService(AgentRunRepository runRepository,
                                         AgentRuntimeLeasePort runtimeLease) {
        this(runRepository, runtimeLease, Clock.systemUTC(), DEFAULT_STALE_MINUTES);
    }

    public AgentRunRecoveryDomainService(AgentRunRepository runRepository,
                                         AgentRuntimeLeasePort runtimeLease,
                                         Clock clock,
                                         int staleMinutes) {
        this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
        this.runtimeLease = Objects.requireNonNull(runtimeLease, "runtimeLease");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (staleMinutes < 1) {
            throw new IllegalArgumentException("staleMinutes must be positive");
        }
        this.staleMinutes = staleMinutes;
    }

    public int recoverStaleRuns() {
        return recoverStaleRuns(OffsetDateTime.now(clock));
    }

    public int recoverStaleRuns(OffsetDateTime now) {
        OffsetDateTime effectiveNow = now == null ? OffsetDateTime.now(clock) : now;
        List<AgentRunBO> candidates = runRepository.findStaleRunningRuns(
                effectiveNow.minusMinutes(staleMinutes));
        int recovered = 0;
        for (AgentRunBO run : candidates) {
            if (run == null || run.getRunNo() == null || runtimeLease.isLeaseActive(run.getRunNo())) {
                continue;
            }
            run.setStatus("FAILED");
            run.setFailureCategory("STALE_RECOVERED");
            run.setErrorMessage(RECOVERY_ERROR);
            run.setFinishedAt(effectiveNow);
            runRepository.updateRun(run);
            recovered++;
        }
        return recovered;
    }
}
