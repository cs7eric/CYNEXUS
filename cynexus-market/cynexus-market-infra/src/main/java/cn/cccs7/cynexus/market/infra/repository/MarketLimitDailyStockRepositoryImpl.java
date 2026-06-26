package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.repository.MarketLimitDailyStockRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketLimitDailyStockBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketLimitDailyStockPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketLimitDailyStockRepositoryImpl implements MarketLimitDailyStockRepository {

    private final MarketLimitDailyStockBasicService basicService;

    public MarketLimitDailyStockRepositoryImpl(MarketLimitDailyStockBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MarketLimitDailyStockBO> listAll() {
        return MarketLimitDailyStockPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public MarketLimitDailyStockBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(MarketLimitDailyStockPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MarketLimitDailyStockBO value) {
        return basicService.create(MarketLimitDailyStockPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MarketLimitDailyStockBO value) {
        return basicService.update(MarketLimitDailyStockPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
