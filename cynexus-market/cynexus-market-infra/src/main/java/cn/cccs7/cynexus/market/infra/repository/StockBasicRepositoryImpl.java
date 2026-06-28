package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import cn.cccs7.cynexus.market.domain.repository.StockBasicRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StockBasicBasicService;
import cn.cccs7.cynexus.market.infra.convert.StockBasicPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockBasicRepositoryImpl implements StockBasicRepository {

    private final StockBasicBasicService stockBasicBasicService;

    public StockBasicRepositoryImpl(StockBasicBasicService stockBasicBasicService) {
        this.stockBasicBasicService = stockBasicBasicService;
    }

    @Override
    public List<StockBasicBO> list() {
        return StockBasicPOConverter.INSTANCE.toTargetList(stockBasicBasicService.listAll());
    }

    @Override
    public StockBasicBO findById(Long id) {
        return Optional.ofNullable(stockBasicBasicService.getById(id))
                .map(StockBasicPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StockBasicBO value) {
        return stockBasicBasicService.create(StockBasicPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StockBasicBO value) {
        return stockBasicBasicService.update(StockBasicPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return stockBasicBasicService.delete(id);
    }
}
