package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.service.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.service.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.service.impl.ModelRoutingDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.service.impl.OutputValidationDomainServiceImpl;
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
}
