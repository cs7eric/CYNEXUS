package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StockFundFlowSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.StockFundFlowSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockFundFlowSnapshotRepositoryImpl implements StockFundFlowSnapshotRepository {

    private final StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService;

    public StockFundFlowSnapshotRepositoryImpl(StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService) {
        this.stockFundFlowSnapshotBasicService = stockFundFlowSnapshotBasicService;
    }

    @Override
    public List<StockFundFlowSnapshotBO> list() {
        return StockFundFlowSnapshotPOConverter.INSTANCE.toTargetList(stockFundFlowSnapshotBasicService.listAll());
    }

    @Override
    public StockFundFlowSnapshotBO findById(Long id) {
        return Optional.ofNullable(stockFundFlowSnapshotBasicService.getById(id))
                .map(StockFundFlowSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StockFundFlowSnapshotBO value) {
        return stockFundFlowSnapshotBasicService.create(StockFundFlowSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StockFundFlowSnapshotBO value) {
        return stockFundFlowSnapshotBasicService.update(StockFundFlowSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return stockFundFlowSnapshotBasicService.delete(id);
    }
}
