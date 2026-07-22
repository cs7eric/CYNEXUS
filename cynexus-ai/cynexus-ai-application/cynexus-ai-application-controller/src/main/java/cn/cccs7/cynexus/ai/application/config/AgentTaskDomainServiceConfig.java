package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.port.agent.AgentRunCancellationPort;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentTaskRepository;
import cn.cccs7.cynexus.ai.domain.service.agent.task.AgentTaskLifecycleDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentTaskDomainServiceConfig {
    @Bean
    public AgentTaskLifecycleDomainService agentTaskLifecycleDomainService(
            AgentTaskRepository repository, AgentRunCancellationPort runCancellationPort) {
        return new AgentTaskLifecycleDomainService(repository, runCancellationPort);
    }
}
