package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentFeedbackRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.service.agent.maintenance.AgentRunRecoveryDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.maintenance.AgentRunRetentionDomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentMaintenanceDomainServiceConfig {
    @Bean
    public AgentRunRecoveryDomainService agentRunRecoveryDomainService(
            AgentRunRepository runRepository, AgentRuntimeLeasePort runtimeLease) {
        return new AgentRunRecoveryDomainService(runRepository, runtimeLease);
    }

    @Bean
    public AgentRunRetentionDomainService agentRunRetentionDomainService(
            AgentRunRepository runRepository,
            AgentFeedbackRepository feedbackRepository,
            @Value("${cynexus.ai.agent.retention-days:90}") int retentionDays) {
        return new AgentRunRetentionDomainService(runRepository, feedbackRepository, retentionDays);
    }
}
