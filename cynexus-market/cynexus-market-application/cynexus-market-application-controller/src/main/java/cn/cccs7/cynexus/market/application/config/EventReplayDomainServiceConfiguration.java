package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.factorresearch.EventReplayDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.EventReplayDomainServiceImpl;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventReplayDomainServiceConfiguration {
    @Bean
    public EventReplayDomainService eventReplayDomainService(StockChartQueryDomainService stockChartQueryDomainService,
                                                             MarketIndicatorQueryDomainService indicatorService) {
        return new EventReplayDomainServiceImpl(stockChartQueryDomainService, indicatorService);
    }
}
