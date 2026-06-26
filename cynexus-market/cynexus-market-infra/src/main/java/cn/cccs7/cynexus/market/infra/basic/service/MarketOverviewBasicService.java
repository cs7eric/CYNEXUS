package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import java.util.Optional;

public interface MarketOverviewBasicService {

    Optional<MarketOverviewPO> findByTradeDate(String tradeDate);
}
