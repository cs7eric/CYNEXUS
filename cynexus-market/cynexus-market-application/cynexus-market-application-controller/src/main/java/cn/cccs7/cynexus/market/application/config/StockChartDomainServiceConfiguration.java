package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.stock.impl.StockChartQueryDomainServiceImpl;
import cn.cccs7.cynexus.market.domain.stock.StockChartDomainService;
import cn.cccs7.cynexus.market.domain.stock.impl.StockChartDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockChartDomainServiceConfiguration {

    @Bean
    public StockChartDomainService stockChartDomainService() {
        return new StockChartDomainServiceImpl();
    }

    @Bean
    public StockChartQueryDomainService stockChartQueryDomainService(
            StockChartQueryRepository repository,
            StockChartDomainService rules) {
        return new StockChartQueryDomainServiceImpl(repository, rules);
    }
}
