package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.service.MarketOverviewBasicService;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketOverviewBasicServiceImpl implements MarketOverviewBasicService {

    @Override
    public Optional<MarketOverviewPO> findByTradeDate(String tradeDate) {
        MarketOverviewPO po = new MarketOverviewPO();
        po.setTradeDate(tradeDate);
        po.setIndexChangePercent(new BigDecimal("0.72"));
        po.setTurnoverAmount(new BigDecimal("9800.00"));
        po.setNorthboundNetInflow(new BigDecimal("18.60"));
        po.setRiseCount(3260);
        po.setFallCount(1870);
        po.setSummary("mock market overview");
        return Optional.of(po);
    }
}
