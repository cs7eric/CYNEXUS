package cn.cccs7.cynexus.core.infra.config;

import cn.cccs7.cynexus.core.domain.client.DataJobExecutorClient;
import cn.cccs7.cynexus.core.domain.repository.SchedulerRepository;
import cn.cccs7.cynexus.core.domain.coreinfo.CoreServiceInfoDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.coreinfo.impl.CoreServiceInfoDomainServiceImpl;
import cn.cccs7.cynexus.core.domain.scheduler.impl.SchedulerDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreDomainConfig {

    @Bean
    public CoreServiceInfoDomainService coreServiceInfoDomainService() {
        return new CoreServiceInfoDomainServiceImpl();
    }

    @Bean
    public SchedulerDomainService schedulerDomainService(SchedulerRepository schedulerRepository,
                                                         DataJobExecutorClient dataJobExecutorClient) {
        return new SchedulerDomainServiceImpl(schedulerRepository, dataJobExecutorClient);
    }
}
