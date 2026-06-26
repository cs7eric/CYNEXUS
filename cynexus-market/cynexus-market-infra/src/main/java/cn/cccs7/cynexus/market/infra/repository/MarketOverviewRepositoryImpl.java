package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketOverviewRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketOverviewBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketOverviewPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketOverviewRepositoryImpl implements MarketOverviewRepository {

    private final MarketOverviewBasicService marketOverviewBasicService;

    public MarketOverviewRepositoryImpl(MarketOverviewBasicService marketOverviewBasicService) {
        this.marketOverviewBasicService = marketOverviewBasicService;
    }

    @Override
    public List<MarketOverviewBO> listAll() {
        return MarketOverviewPOConverter.INSTANCE.toTargetList(marketOverviewBasicService.listAll());
    }

    @Override
    public MarketOverviewBO findById(Long id) {
        return Optional.ofNullable(marketOverviewBasicService.getById(id))
                .map(MarketOverviewPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MarketOverviewBO value) {
        return marketOverviewBasicService.create(MarketOverviewPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MarketOverviewBO value) {
        return marketOverviewBasicService.update(MarketOverviewPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return marketOverviewBasicService.delete(id);
    }

    @Override
    public Optional<MarketOverviewBO> findByTradeDate(String tradeDate) {
        return marketOverviewBasicService.findByTradeDate(tradeDate).map(MarketOverviewPOConverter.INSTANCE::toTarget);
    }
}
