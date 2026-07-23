package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketOverviewQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class MarketOverviewAppService {

    private final MarketOverviewQueryDomainService domainService;

    public MarketOverviewAppService(MarketOverviewQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public MarketOverviewBO getOverview(String tradeDate) {
        return domainService.getOverview(tradeDate);
    }
}
