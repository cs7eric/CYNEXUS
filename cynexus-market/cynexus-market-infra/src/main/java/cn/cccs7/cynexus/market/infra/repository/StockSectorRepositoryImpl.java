package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import cn.cccs7.cynexus.market.domain.repository.StockSectorRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StockSectorBasicService;
import cn.cccs7.cynexus.market.infra.convert.StockSectorPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockSectorRepositoryImpl implements StockSectorRepository {

    private final StockSectorBasicService stockSectorBasicService;

    public StockSectorRepositoryImpl(StockSectorBasicService stockSectorBasicService) {
        this.stockSectorBasicService = stockSectorBasicService;
    }

    @Override
    public List<StockSectorBO> list() {
        return StockSectorPOConverter.INSTANCE.toTargetList(stockSectorBasicService.listAll());
    }

    @Override
    public StockSectorBO findById(Long id) {
        return Optional.ofNullable(stockSectorBasicService.getById(id))
                .map(StockSectorPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StockSectorBO value) {
        return stockSectorBasicService.create(StockSectorPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StockSectorBO value) {
        return stockSectorBasicService.update(StockSectorPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return stockSectorBasicService.delete(id);
    }
}
