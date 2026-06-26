package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import org.springframework.stereotype.Service;

@Service
public class MarketOverviewAppService {

    private final MarketOverviewRepository marketOverviewRepository;

    public MarketOverviewAppService(MarketOverviewRepository marketOverviewRepository) {
        this.marketOverviewRepository = marketOverviewRepository;
    }

    public MarketOverviewBO getOverview(String tradeDate) {
        return marketOverviewRepository.findByTradeDate(tradeDate)
                .orElseThrow(() -> new IllegalArgumentException("Market overview not found"));
    }
}
