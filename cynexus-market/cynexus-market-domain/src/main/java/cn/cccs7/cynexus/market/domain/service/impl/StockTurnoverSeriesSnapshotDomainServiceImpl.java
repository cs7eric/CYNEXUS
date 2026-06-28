package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StockTurnoverSeriesSnapshotRepository;
import cn.cccs7.cynexus.market.domain.service.StockTurnoverSeriesSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockTurnoverSeriesSnapshotDomainServiceImpl implements StockTurnoverSeriesSnapshotDomainService {

    private final StockTurnoverSeriesSnapshotRepository stockTurnoverSeriesSnapshotRepository;

    public StockTurnoverSeriesSnapshotDomainServiceImpl(StockTurnoverSeriesSnapshotRepository stockTurnoverSeriesSnapshotRepository) {
        this.stockTurnoverSeriesSnapshotRepository = stockTurnoverSeriesSnapshotRepository;
    }

    @Override
    public List<StockTurnoverSeriesSnapshotBO> list() {
        return stockTurnoverSeriesSnapshotRepository.list();
    }

    @Override
    public StockTurnoverSeriesSnapshotBO findById(Long id) {
        return stockTurnoverSeriesSnapshotRepository.findById(id);
    }

    @Override
    public Long create(StockTurnoverSeriesSnapshotBO value) {
        return stockTurnoverSeriesSnapshotRepository.create(value);
    }

    @Override
    public boolean update(StockTurnoverSeriesSnapshotBO value) {
        return stockTurnoverSeriesSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return stockTurnoverSeriesSnapshotRepository.delete(id);
    }
}
