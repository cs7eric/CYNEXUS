package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiDomainService;
import cn.cccs7.cynexus.ai.domain.marketindicator.MarketIndicatorReplayAiDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketIndicatorReplayAiDomainServiceConfig {
    @Bean
    public MarketIndicatorReplayAiDomainService marketIndicatorReplayAiDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            MarketIndicatorReplayAiCompletionPort completionPort) {
        return new MarketIndicatorReplayAiDomainServiceImpl(routingService, promptRepository, executionLogRepository, completionPort);
    }
}
