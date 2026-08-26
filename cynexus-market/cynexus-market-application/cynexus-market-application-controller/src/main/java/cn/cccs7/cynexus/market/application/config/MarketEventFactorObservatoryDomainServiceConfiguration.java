package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.domain.repository.StockScreenerRepository;
import cn.cccs7.cynexus.market.domain.service.factor.MarketEventFactorObservatoryDomainService;
import cn.cccs7.cynexus.market.domain.service.factor.impl.MarketEventFactorObservatoryDomainServiceImpl;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketPulseQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketEventFactorObservatoryDomainServiceConfiguration {

    @Bean
    public MarketEventFactorObservatoryDomainService marketEventFactorObservatoryDomainService(
            MarketPulseQueryDomainService pulseService, StockScreenerRepository screenerRepository,
            MarketSentimentQueryDomainService sentimentService, MarketIndicatorQueryDomainService indicatorService,
            MarginTradingRepository marginRepository) {
        return new MarketEventFactorObservatoryDomainServiceImpl(pulseService, screenerRepository, sentimentService,
                indicatorService, marginRepository);
    }
}
