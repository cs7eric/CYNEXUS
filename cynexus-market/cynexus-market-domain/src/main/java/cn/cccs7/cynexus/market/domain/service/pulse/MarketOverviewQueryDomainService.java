package cn.cccs7.cynexus.market.domain.service.pulse;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;

public interface MarketOverviewQueryDomainService {

    MarketOverviewBO getOverview(String tradeDate);
}
