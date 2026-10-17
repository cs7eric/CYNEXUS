package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorAnalysisCompletionPort;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorAnalysisDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.MarketEventFactorEvidencePort;
import cn.cccs7.cynexus.ai.domain.marketfactor.impl.MarketEventFactorAnalysisDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketEventFactorAnalysisDomainServiceConfig {
    @Bean
    public MarketEventFactorAnalysisDomainService marketEventFactorAnalysisDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiExecutionLogRepository executionLogRepository,
            MarketEventFactorEvidencePort evidencePort,
            MarketEventFactorAnalysisCompletionPort completionPort,
            AiPromptRuntimeResolver promptRuntimeResolver,
            AiStructuredOutputParser structuredOutputParser) {
        return new MarketEventFactorAnalysisDomainServiceImpl(routingService, executionLogRepository, evidencePort, completionPort, promptRuntimeResolver, structuredOutputParser);
    }
}
