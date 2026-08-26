package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiCandidatePort;
import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.stockselection.StockSelectionAiDomainService;
import cn.cccs7.cynexus.ai.domain.stockselection.impl.StockSelectionAiDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockSelectionAiDomainServiceConfig {

    @Bean
    public StockSelectionAiDomainService stockSelectionAiDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            StockSelectionAiCandidatePort candidatePort,
            StockSelectionAiCompletionPort completionPort) {
        return new StockSelectionAiDomainServiceImpl(routingService, promptRepository, executionLogRepository,
                candidatePort, completionPort);
    }
}
