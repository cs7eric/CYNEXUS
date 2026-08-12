package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketIndicatorQueryDomainServiceConfiguration {
    @Bean
    public MarketIndicatorQueryDomainService marketIndicatorQueryDomainService(MarketIndicatorRepository repository,
                                                                                 MarketPulseQueryRepository pulseRepository) {
        return new MarketIndicatorQueryDomainService(repository, pulseRepository);
    }
}
