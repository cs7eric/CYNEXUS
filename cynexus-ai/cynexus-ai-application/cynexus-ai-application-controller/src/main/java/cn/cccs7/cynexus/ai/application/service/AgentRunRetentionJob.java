package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentRunRetentionJob {
    private final AgentRunRepository repository;
    private final int retentionDays;

    public AgentRunRetentionJob(AgentRunRepository repository,
                                @Value("${cynexus.ai.agent.retention-days:90}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = Math.max(7, retentionDays);
    }

    @Scheduled(initialDelayString = "${cynexus.ai.agent.retention-initial-delay-ms:120000}",
            fixedDelayString = "${cynexus.ai.agent.retention-delay-ms:86400000}")
    public void archiveExpiredRuns() {
        repository.archiveBefore(OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays));
    }
}
