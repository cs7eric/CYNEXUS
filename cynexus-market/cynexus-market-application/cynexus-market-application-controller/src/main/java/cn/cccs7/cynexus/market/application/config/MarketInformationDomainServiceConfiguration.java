package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.MarketInformationRepository;
import cn.cccs7.cynexus.market.domain.service.information.MarketInformationQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.information.impl.MarketInformationQueryDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketInformationDomainServiceConfiguration {

    @Bean
    public MarketInformationQueryDomainService marketInformationQueryDomainService(
            MarketInformationRepository repository) {
        return new MarketInformationQueryDomainServiceImpl(repository);
    }
}
