package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StockTurnoverSeriesSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.StockTurnoverSeriesSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.StockTurnoverSeriesSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StockTurnoverSeriesSnapshotRepositoryImpl implements StockTurnoverSeriesSnapshotRepository {

    private final StockTurnoverSeriesSnapshotBasicService stockTurnoverSeriesSnapshotBasicService;

    public StockTurnoverSeriesSnapshotRepositoryImpl(StockTurnoverSeriesSnapshotBasicService stockTurnoverSeriesSnapshotBasicService) {
        this.stockTurnoverSeriesSnapshotBasicService = stockTurnoverSeriesSnapshotBasicService;
    }

    @Override
    public List<StockTurnoverSeriesSnapshotBO> list() {
        return StockTurnoverSeriesSnapshotPOConverter.INSTANCE.toTargetList(stockTurnoverSeriesSnapshotBasicService.listAll());
    }

    @Override
    public StockTurnoverSeriesSnapshotBO findById(Long id) {
        return Optional.ofNullable(stockTurnoverSeriesSnapshotBasicService.getById(id))
                .map(StockTurnoverSeriesSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(StockTurnoverSeriesSnapshotBO value) {
        return stockTurnoverSeriesSnapshotBasicService.create(StockTurnoverSeriesSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(StockTurnoverSeriesSnapshotBO value) {
        return stockTurnoverSeriesSnapshotBasicService.update(StockTurnoverSeriesSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return stockTurnoverSeriesSnapshotBasicService.delete(id);
    }
}
