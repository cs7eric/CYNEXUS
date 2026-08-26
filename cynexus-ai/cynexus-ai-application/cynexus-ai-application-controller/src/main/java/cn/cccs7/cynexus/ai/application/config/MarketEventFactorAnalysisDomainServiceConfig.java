package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorAnalysisCompletionPort;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorAnalysisDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorEvidencePort;
import cn.cccs7.cynexus.ai.domain.marketfactor.impl.MarketEventFactorAnalysisDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketEventFactorAnalysisDomainServiceConfig {
    @Bean
    public MarketEventFactorAnalysisDomainService marketEventFactorAnalysisDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            MarketEventFactorEvidencePort evidencePort,
            MarketEventFactorAnalysisCompletionPort completionPort) {
        return new MarketEventFactorAnalysisDomainServiceImpl(routingService, promptRepository, executionLogRepository, evidencePort, completionPort);
    }
}
