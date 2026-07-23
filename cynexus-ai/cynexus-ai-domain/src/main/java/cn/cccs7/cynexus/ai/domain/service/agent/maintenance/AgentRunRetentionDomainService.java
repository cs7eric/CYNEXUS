package cn.cccs7.cynexus.ai.domain.service.agent.maintenance;

import cn.cccs7.cynexus.ai.domain.repository.agent.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/** Owns retention policy and coordinates archival of run data and feedback. */
public final class AgentRunRetentionDomainService {
    private static final int MIN_RETENTION_DAYS = 7;

    private final AgentRunRepository runRepository;
    private final AgentFeedbackRepository feedbackRepository;
    private final Clock clock;
    private final int retentionDays;

    public AgentRunRetentionDomainService(AgentRunRepository runRepository,
                                          AgentFeedbackRepository feedbackRepository,
                                          int retentionDays) {
        this(runRepository, feedbackRepository, Clock.systemUTC(), retentionDays);
    }

    public AgentRunRetentionDomainService(AgentRunRepository runRepository,
                                          AgentFeedbackRepository feedbackRepository,
                                          Clock clock,
                                          int retentionDays) {
        this.runRepository = Objects.requireNonNull(runRepository, "runRepository");
        this.feedbackRepository = Objects.requireNonNull(feedbackRepository, "feedbackRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.retentionDays = Math.max(MIN_RETENTION_DAYS, retentionDays);
    }

    public RetentionResult archiveExpiredRuns() {
        return archiveExpiredRuns(OffsetDateTime.now(clock));
    }

    public RetentionResult archiveExpiredRuns(OffsetDateTime now) {
        OffsetDateTime effectiveNow = now == null ? OffsetDateTime.now(clock) : now;
        OffsetDateTime cutoff = effectiveNow.withOffsetSameInstant(ZoneOffset.UTC)
                .minusDays(retentionDays);
        int archivedRuns = runRepository.archiveBefore(cutoff);
        int archivedFeedback = feedbackRepository.archiveBefore(cutoff);
        return new RetentionResult(cutoff, archivedRuns, archivedFeedback);
    }

    public record RetentionResult(OffsetDateTime cutoff, int archivedRuns, int archivedFeedback) {
    }
}
