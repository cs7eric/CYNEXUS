package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportAssetStorePort;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDispatchPort;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDomainService;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortfolioDomainServiceConfiguration {
    @Bean
    public PortfolioImportDomainService portfolioImportDomainService(
            PortfolioIdGenerator idGenerator,
            PortfolioImportAssetStorePort assetStore,
            PortfolioImportTaskRepository taskRepository,
            PortfolioImportDispatchPort dispatchPort) {
        return new PortfolioImportDomainService(idGenerator, assetStore, taskRepository, dispatchPort);
    }
}
