package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisCompletionPort;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisKlineInputPort;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisPageDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.impl.ApplicationAnalysisExecutionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.impl.ApplicationAnalysisPageDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationAnalysisDomainServiceConfig {

    @Bean
    public ApplicationAnalysisExecutionDomainService applicationAnalysisExecutionDomainService(
            ApplicationAnalysisPageRepository pageRepository,
            ApplicationAnalysisRoutingDomainService routingService,
            ApplicationAnalysisCompletionPort completionPort,
            ApplicationAnalysisStructuredOutputPort structuredOutputPort,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository) {
        return new ApplicationAnalysisExecutionDomainServiceImpl(
                pageRepository, routingService, completionPort, structuredOutputPort,
                promptRepository, executionLogRepository);
    }

    @Bean
    public ApplicationAnalysisPageDomainService applicationAnalysisPageDomainService(
            ApplicationAnalysisPageRepository pageRepository,
            ApplicationAnalysisExecutionDomainService executionService,
            ApplicationAnalysisKlineInputPort klineInputPort) {
        return new ApplicationAnalysisPageDomainServiceImpl(pageRepository, executionService, klineInputPort);
    }
}
