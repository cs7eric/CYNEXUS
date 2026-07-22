package cn.cccs7.cynexus.core.application.config;

import cn.cccs7.cynexus.core.application.service.SchedulerDataJobKeyResolver;
import cn.cccs7.cynexus.core.domain.client.MarketAutomationClient;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.gateway.scheduler.SchedulerExecutionCapabilityPort;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerTimingDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerExecutionDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerManagementDomainService;
import cn.cccs7.cynexus.core.domain.service.scheduler.impl.SchedulerExecutionDomainServiceImpl;
import cn.cccs7.cynexus.core.domain.service.scheduler.impl.SchedulerManagementDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerDomainServiceConfiguration {

    @Bean
    public SchedulerManagementDomainService schedulerManagementDomainService(
            SchedulerDomainService schedulerDomainService) {
        return new SchedulerManagementDomainServiceImpl(schedulerDomainService);
    }

    @Bean
    public SchedulerExecutionCapabilityPort schedulerExecutionCapabilityPort() {
        return new SchedulerExecutionCapabilityPort() {
            @Override
            public boolean supports(SchedulerJobBO job) {
                return isMarketAutomation(job) || SchedulerDataJobKeyResolver.resolve(job == null ? null : job.getCode()).isPresent();
            }

            @Override
            public boolean isMarketAutomation(SchedulerJobBO job) {
                return job != null && "market_information_pipeline_refresh".equals(job.getCode());
            }
        };
    }

    @Bean
    public SchedulerExecutionDomainService schedulerExecutionDomainService(
            SchedulerDomainService schedulerDomainService,
            SchedulerTimingDomainService timingDomainService,
            SchedulerExecutionCapabilityPort capabilityPort,
            MarketAutomationClient marketAutomationClient) {
        return new SchedulerExecutionDomainServiceImpl(
                schedulerDomainService, timingDomainService, capabilityPort, marketAutomationClient);
    }
}
