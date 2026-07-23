package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.etf.MarketEtfDomainService;
import cn.cccs7.cynexus.market.domain.repository.MarketEtfQueryRepository;
import cn.cccs7.cynexus.market.domain.service.etf.MarketEtfQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.etf.impl.MarketEtfQueryDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketEtfDomainServiceConfiguration {

    @Bean
    public MarketEtfDomainService marketEtfDomainService() {
        return new MarketEtfDomainService();
    }

    @Bean
    public MarketEtfQueryDomainService marketEtfQueryDomainService(
            MarketEtfQueryRepository repository,
            MarketEtfDomainService marketEtfDomainService) {
        return new MarketEtfQueryDomainServiceImpl(repository, marketEtfDomainService);
    }
}
