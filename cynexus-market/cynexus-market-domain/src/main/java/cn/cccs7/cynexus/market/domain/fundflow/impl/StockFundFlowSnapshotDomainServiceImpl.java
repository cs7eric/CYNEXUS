package cn.cccs7.cynexus.market.domain.fundflow.impl;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.StockFundFlowSnapshotRepository;
import cn.cccs7.cynexus.market.domain.fundflow.StockFundFlowSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowSnapshotDomainServiceImpl implements StockFundFlowSnapshotDomainService {

    private final StockFundFlowSnapshotRepository stockFundFlowSnapshotRepository;

    public StockFundFlowSnapshotDomainServiceImpl(StockFundFlowSnapshotRepository stockFundFlowSnapshotRepository) {
        this.stockFundFlowSnapshotRepository = stockFundFlowSnapshotRepository;
    }

    @Override
    public List<StockFundFlowSnapshotBO> list() {
        return stockFundFlowSnapshotRepository.list();
    }

    @Override
    public StockFundFlowSnapshotBO findById(Long id) {
        return stockFundFlowSnapshotRepository.findById(id);
    }

    @Override
    public Long create(StockFundFlowSnapshotBO value) {
        return stockFundFlowSnapshotRepository.create(value);
    }

    @Override
    public boolean update(StockFundFlowSnapshotBO value) {
        return stockFundFlowSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return stockFundFlowSnapshotRepository.delete(id);
    }
}
