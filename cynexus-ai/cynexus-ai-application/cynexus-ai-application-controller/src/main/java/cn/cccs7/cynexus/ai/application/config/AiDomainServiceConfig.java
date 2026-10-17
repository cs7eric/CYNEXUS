package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.repository.AgentQuotaPolicyRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisConfigRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisDailySnapshotRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultCurrentRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisResultHistoryRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisTargetRepository;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolCatalog;
import cn.cccs7.cynexus.ai.domain.gateway.capability.AgentToolExecutor;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import cn.cccs7.cynexus.ai.domain.gateway.observability.AgentObservabilityPort;
import cn.cccs7.cynexus.ai.domain.gateway.quota.AgentQuotaPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentCircuitBreakerPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.gateway.runtime.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentRunDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentRunExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.execution.AgentTurnDomainService;
import cn.cccs7.cynexus.ai.domain.service.agent.prompt.AgentSystemPromptDomainService;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainService;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainService;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.prompt.PromptRenderDomainService;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainService;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.scenario.ScenarioExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.service.scenario.ScenarioExecutionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.service.usage.AiUsageQueryDomainService;
import cn.cccs7.cynexus.ai.domain.service.usage.AiUsageQueryDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageObservabilityRepository;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiDomainServiceConfig {

    @Bean
    public ModelRoutingDomainService modelRoutingDomainService() {
        return new ModelRoutingDomainServiceImpl();
    }

    @Bean
    public ModelRouteDomainService modelRouteDomainService(
            AiModelRepository aiModelRepository,
            ModelRoutingDomainService modelRoutingDomainService) {
        return new ModelRouteDomainServiceImpl(aiModelRepository, modelRoutingDomainService);
    }

    @Bean
    public OutputValidationDomainService outputValidationDomainService() {
        return new OutputValidationDomainServiceImpl();
    }

    @Bean
    public PromptRenderDomainService promptRenderDomainService() {
        return new PromptRenderDomainService();
    }

    @Bean
    public AiModelQueryDomainService aiModelQueryDomainService(AiModelRepository aiModelRepository) {
        return new AiModelQueryDomainServiceImpl(aiModelRepository);
    }

    @Bean
    public AiUsageQueryDomainService aiUsageQueryDomainService(
            AiUsageObservabilityRepository usageObservabilityRepository,
            AiProviderRepository providerRepository) {
        return new AiUsageQueryDomainServiceImpl(usageObservabilityRepository, providerRepository);
    }

    @Bean
    public AgentTurnDomainService agentTurnDomainService(
            AgentCircuitBreakerPort circuitBreakerPort,
            AgentObservabilityPort observabilityPort) {
        return new AgentTurnDomainService(
                circuitBreakerPort,
                observabilityPort,
                AgentExecutionLimits.defaults().getMaxRetryAttempts());
    }

    @Bean
    public AgentExecutionDomainService agentExecutionDomainService(AgentTurnDomainService agentTurnDomainService) {
        return new AgentExecutionDomainService(AgentExecutionLimits.defaults(), agentTurnDomainService);
    }

    @Bean
    public AgentRunExecutionDomainService agentRunExecutionDomainService(
            AgentRunDomainService runDomainService,
            AgentToolCatalog toolCatalog,
            AgentToolExecutor toolExecutor,
            AiCompletionGateway completionGateway,
            ObjectMapper objectMapper,
            AgentRuntimeLeasePort runtimeLeasePort,
            AgentRateLimitPort rateLimitPort,
            AgentQuotaPort quotaPort,
            AgentObservabilityPort observabilityPort,
            AgentExecutionDomainService executionDomainService,
            AgentTurnDomainService turnDomainService,
            AgentSystemPromptDomainService systemPromptDomainService) {
        return new AgentRunExecutionDomainService(
                runDomainService,
                toolCatalog,
                toolExecutor,
                completionGateway,
                objectMapper,
                runtimeLeasePort,
                rateLimitPort,
                quotaPort,
                observabilityPort,
                executionDomainService,
                turnDomainService,
                systemPromptDomainService);
    }

    @Bean
    public ScenarioExecutionDomainService scenarioExecutionDomainService(
            ApplicationAnalysisRoutingDomainService routingDomainService,
            ModelRouteDomainService modelRouteDomainService,
            OutputValidationDomainService outputValidationDomainService,
            AiPromptRuntimeResolver promptRuntimeResolver,
            cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser structuredOutputParser) {
        return new ScenarioExecutionDomainServiceImpl(
                routingDomainService,
                modelRouteDomainService,
                outputValidationDomainService,
                promptRuntimeResolver,
                structuredOutputParser);
    }

    @Bean
    public AiConfigurationDomainService aiConfigurationDomainService(
            AiProviderRepository providerRepository,
            AiPromptRepository promptRepository,
            AiUsageBindingRepository usageBindingRepository,
            ApplicationAnalysisConfigRepository analysisConfigRepository,
            ApplicationAnalysisDailySnapshotRepository dailySnapshotRepository,
            ApplicationAnalysisResultCurrentRepository resultCurrentRepository,
            ApplicationAnalysisResultHistoryRepository resultHistoryRepository,
            ApplicationAnalysisTargetRepository targetRepository,
            AgentQuotaPolicyRepository quotaPolicyRepository) {
        return new AiConfigurationDomainServiceImpl(
                providerRepository,
                promptRepository,
                usageBindingRepository,
                analysisConfigRepository,
                dailySnapshotRepository,
                resultCurrentRepository,
                resultHistoryRepository,
                targetRepository,
                quotaPolicyRepository);
    }
}
