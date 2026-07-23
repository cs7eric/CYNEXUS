package cn.cccs7.cynexus.market.domain.service.etf;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfBO;

public interface MarketEtfQueryDomainService {

    MarketEtfBO getMarket(int historyDays, int rowLimit);
}
