package cn.cccs7.cynexus.ai.application.job.agent;

import cn.cccs7.cynexus.ai.domain.service.agent.maintenance.AgentRunRetentionDomainService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentRunRetentionJob {
    private final AgentRunRetentionDomainService retentionDomainService;

    public AgentRunRetentionJob(AgentRunRetentionDomainService retentionDomainService) {
        this.retentionDomainService = retentionDomainService;
    }

    @Scheduled(initialDelayString = "${cynexus.ai.agent.retention-initial-delay-ms:120000}",
            fixedDelayString = "${cynexus.ai.agent.retention-delay-ms:86400000}")
    public void archiveExpiredRuns() {
        retentionDomainService.archiveExpiredRuns();
    }
}
