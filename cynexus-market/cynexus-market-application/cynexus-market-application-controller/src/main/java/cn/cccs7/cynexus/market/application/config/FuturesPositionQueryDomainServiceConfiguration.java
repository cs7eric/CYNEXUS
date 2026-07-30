package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.FuturesPositionRepository;
import cn.cccs7.cynexus.market.domain.service.futures.FuturesPositionQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.futures.FuturesPositionQueryDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FuturesPositionQueryDomainServiceConfiguration {
    @Bean
    public FuturesPositionQueryDomainService futuresPositionQueryDomainService(FuturesPositionRepository repository) {
        return new FuturesPositionQueryDomainServiceImpl(repository);
    }
}
