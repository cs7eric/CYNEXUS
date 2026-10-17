package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketinformation.MarketInformationAiDomainService;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import cn.cccs7.cynexus.ai.domain.port.marketinformation.MarketInformationAiCompletionPort;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.MarketInformationAiTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Boot wiring for the market-information domain service. */
@Configuration
public class MarketInformationAiDomainServiceConfig {

    @Bean
    public MarketInformationAiDomainService marketInformationAiDomainService(
            MarketInformationAiTaskRepository taskRepository,
            ApplicationAnalysisRoutingDomainService routingService,
            AiExecutionLogRepository executionLogRepository,
            MarketInformationAiCompletionPort completionPort,
            AiPromptRuntimeResolver promptRuntimeResolver,
            AiStructuredOutputParser structuredOutputParser,
            @Value("${cynexus.ai.market-information.deepseek-live-limit:0}") int deepSeekLiveLimit,
            @Value("${cynexus.ai.market-information.task-timeout-minutes:10}") int taskTimeoutMinutes) {
        return new MarketInformationAiDomainService(taskRepository, routingService,
                executionLogRepository, completionPort, promptRuntimeResolver,
                structuredOutputParser, deepSeekLiveLimit, taskTimeoutMinutes);
    }
}
