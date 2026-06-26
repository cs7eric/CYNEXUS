package cn.cccs7.cynexus.core.infra.config;

import cn.cccs7.cynexus.core.domain.service.CoreServiceInfoDomainService;
import cn.cccs7.cynexus.core.domain.service.impl.CoreServiceInfoDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreDomainConfig {

    @Bean
    public CoreServiceInfoDomainService coreServiceInfoDomainService() {
        return new CoreServiceInfoDomainServiceImpl();
    }
}
