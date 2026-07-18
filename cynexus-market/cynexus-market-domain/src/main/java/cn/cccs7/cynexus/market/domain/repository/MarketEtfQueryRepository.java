package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketEtfDataBO;

public interface MarketEtfQueryRepository {

    MarketEtfDataBO load(int historyDays, int rowLimit);
}
