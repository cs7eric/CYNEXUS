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
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainService;
import cn.cccs7.cynexus.ai.domain.service.configuration.AiConfigurationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainService;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiDomainServiceConfig {

    @Bean
    public ModelRoutingDomainService modelRoutingDomainService() {
        return new ModelRoutingDomainServiceImpl();
    }

    @Bean
    public OutputValidationDomainService outputValidationDomainService() {
        return new OutputValidationDomainServiceImpl();
    }

    @Bean
    public AiModelQueryDomainService aiModelQueryDomainService(AiModelRepository aiModelRepository) {
        return new AiModelQueryDomainServiceImpl(aiModelRepository);
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
