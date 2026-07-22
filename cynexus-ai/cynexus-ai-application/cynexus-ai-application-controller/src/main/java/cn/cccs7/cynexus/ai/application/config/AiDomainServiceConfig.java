package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainServiceImpl;
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
}
