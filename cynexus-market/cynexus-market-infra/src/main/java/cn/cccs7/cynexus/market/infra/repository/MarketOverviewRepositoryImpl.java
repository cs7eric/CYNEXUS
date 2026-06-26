package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketOverviewBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketOverviewPOConverter;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketOverviewRepositoryImpl implements MarketOverviewRepository {

    private final MarketOverviewBasicService marketOverviewBasicService;
    private final MarketOverviewPOConverter marketOverviewPOConverter;

    public MarketOverviewRepositoryImpl(MarketOverviewBasicService marketOverviewBasicService,
                                        MarketOverviewPOConverter marketOverviewPOConverter) {
        this.marketOverviewBasicService = marketOverviewBasicService;
        this.marketOverviewPOConverter = marketOverviewPOConverter;
    }

    @Override
    public Optional<MarketOverviewBO> findByTradeDate(String tradeDate) {
        return marketOverviewBasicService.findByTradeDate(tradeDate).map(marketOverviewPOConverter::toTarget);
    }
}
