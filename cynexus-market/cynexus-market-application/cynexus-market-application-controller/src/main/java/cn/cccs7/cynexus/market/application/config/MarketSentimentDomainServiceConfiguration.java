package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.MarketSentimentQueryRepository;
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.sentiment.impl.MarketSentimentQueryDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketSentimentDomainServiceConfiguration {

    @Bean
    public MarketSentimentQueryDomainService marketSentimentQueryDomainService(
            MarketSentimentQueryRepository repository) {
        return new MarketSentimentQueryDomainServiceImpl(repository);
    }
}
