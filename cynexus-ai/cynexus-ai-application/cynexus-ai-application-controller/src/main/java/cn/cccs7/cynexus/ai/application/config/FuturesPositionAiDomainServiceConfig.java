package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.futuresposition.FuturesPositionAiDomainService;
import cn.cccs7.cynexus.ai.domain.futuresposition.impl.FuturesPositionAiDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FuturesPositionAiDomainServiceConfig {

    @Bean
    public FuturesPositionAiDomainService futuresPositionAiDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            AiExecutionLogRepository executionLogRepository,
            FuturesPositionAiCompletionPort completionPort,
            AiPromptRuntimeResolver promptRuntimeResolver,
            AiStructuredOutputParser structuredOutputParser) {
        return new FuturesPositionAiDomainServiceImpl(
                routingService, executionLogRepository, completionPort, promptRuntimeResolver, structuredOutputParser);
    }
}
