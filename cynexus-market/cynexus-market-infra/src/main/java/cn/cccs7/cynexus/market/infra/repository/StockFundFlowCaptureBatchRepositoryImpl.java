package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.StockFundFlowCaptureBatchRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowCaptureBatchBasicService;
import cn.cccs7.cynexus.market.infra.convert.StockFundFlowCaptureBatchPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockFundFlowCaptureBatchRepositoryImpl implements StockFundFlowCaptureBatchRepository {

    private final StockFundFlowCaptureBatchBasicService stockFundFlowCaptureBatchBasicService;

    public StockFundFlowCaptureBatchRepositoryImpl(StockFundFlowCaptureBatchBasicService stockFundFlowCaptureBatchBasicService) {
        this.stockFundFlowCaptureBatchBasicService = stockFundFlowCaptureBatchBasicService;
    }

    @Override
    public List<StockFundFlowCaptureBatchBO> list() {
        return StockFundFlowCaptureBatchPOConverter.INSTANCE.toTargetList(stockFundFlowCaptureBatchBasicService.listAll());
    }

    @Override
    public StockFundFlowCaptureBatchBO findById(Long id) {
        return Optional.ofNullable(stockFundFlowCaptureBatchBasicService.getById(id))
                .map(StockFundFlowCaptureBatchPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StockFundFlowCaptureBatchBO value) {
        return stockFundFlowCaptureBatchBasicService.create(StockFundFlowCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StockFundFlowCaptureBatchBO value) {
        return stockFundFlowCaptureBatchBasicService.update(StockFundFlowCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return stockFundFlowCaptureBatchBasicService.delete(id);
    }
}
