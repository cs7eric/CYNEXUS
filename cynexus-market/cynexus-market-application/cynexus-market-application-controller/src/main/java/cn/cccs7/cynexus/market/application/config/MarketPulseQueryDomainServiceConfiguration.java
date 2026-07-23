package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketPulseQueryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketPulseQueryDomainServiceConfiguration {

    @Bean
    public MarketPulseQueryDomainService marketPulseQueryDomainService(
            MarketPulseQueryRepository marketPulseQueryRepository,
            ThsIndustryConstituentSnapshotRepository constituentRepository) {
        return new MarketPulseQueryDomainService(marketPulseQueryRepository, constituentRepository);
    }
}
