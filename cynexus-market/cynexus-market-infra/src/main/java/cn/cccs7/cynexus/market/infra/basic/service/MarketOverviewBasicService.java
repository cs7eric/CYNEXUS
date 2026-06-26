package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import java.util.Optional;

public interface MarketOverviewBasicService extends CrudBasicService<MarketOverviewPO> {

    Optional<MarketOverviewPO> findByTradeDate(String tradeDate);
}
