package cn.cccs7.cynexus.market.domain.service.etf.impl;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfBO;
import cn.cccs7.cynexus.market.domain.etf.MarketEtfDomainService;
import cn.cccs7.cynexus.market.domain.repository.MarketEtfQueryRepository;
import cn.cccs7.cynexus.market.domain.service.etf.MarketEtfQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class MarketEtfQueryDomainServiceImpl implements MarketEtfQueryDomainService {

    private final MarketEtfQueryRepository repository;
    private final MarketEtfDomainService marketEtfDomainService;

    public MarketEtfQueryDomainServiceImpl(MarketEtfQueryRepository repository,
                                            MarketEtfDomainService marketEtfDomainService) {
        this.repository = repository;
        this.marketEtfDomainService = marketEtfDomainService;
    }

    @Override
    public MarketEtfBO getMarket(int historyDays, int rowLimit) {
        int boundedRowLimit = Math.max(20, Math.min(rowLimit, 300));
        int boundedHistoryDays = Math.max(3, Math.min(historyDays, 30));
        return marketEtfDomainService.build(repository.load(boundedHistoryDays, boundedRowLimit), boundedRowLimit);
    }
}
