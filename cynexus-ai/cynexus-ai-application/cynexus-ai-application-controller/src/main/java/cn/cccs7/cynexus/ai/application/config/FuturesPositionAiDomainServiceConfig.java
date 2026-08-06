package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import cn.cccs7.cynexus.ai.domain.futuresposition.impl.FuturesPositionAiDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FuturesPositionAiDomainServiceConfig {

    @Bean
    public FuturesPositionAiDomainService futuresPositionAiDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            FuturesPositionAiCompletionPort completionPort) {
        return new FuturesPositionAiDomainServiceImpl(
                routingService, promptRepository, executionLogRepository, completionPort);
    }
}
