package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
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
            AiExecutionLogRepository executionLogRepository,
            StockSelectionAiCandidatePort candidatePort,
            StockSelectionAiCompletionPort completionPort,
            AiPromptRuntimeResolver promptRuntimeResolver,
            AiStructuredOutputParser structuredOutputParser) {
        return new StockSelectionAiDomainServiceImpl(routingService, executionLogRepository,
                candidatePort, completionPort, promptRuntimeResolver, structuredOutputParser);
    }
}
