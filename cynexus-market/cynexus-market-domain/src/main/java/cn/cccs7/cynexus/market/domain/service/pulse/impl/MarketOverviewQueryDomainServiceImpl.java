package cn.cccs7.cynexus.market.domain.service.pulse.impl;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketOverviewQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class MarketOverviewQueryDomainServiceImpl implements MarketOverviewQueryDomainService {

    private final MarketOverviewRepository repository;

    public MarketOverviewQueryDomainServiceImpl(MarketOverviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarketOverviewBO getOverview(String tradeDate) {
        if (tradeDate == null || tradeDate.isBlank()) {
            throw new IllegalArgumentException("Trade date is required");
        }
        return repository.findByTradeDate(tradeDate.trim())
                .orElseThrow(() -> new IllegalArgumentException("Market overview not found"));
    }
}
