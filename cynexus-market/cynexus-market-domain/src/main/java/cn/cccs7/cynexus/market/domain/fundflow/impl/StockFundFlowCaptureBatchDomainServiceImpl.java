package cn.cccs7.cynexus.market.domain.fundflow.impl;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.StockFundFlowCaptureBatchRepository;
import cn.cccs7.cynexus.market.domain.fundflow.StockFundFlowCaptureBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockFundFlowCaptureBatchDomainServiceImpl implements StockFundFlowCaptureBatchDomainService {

    private final StockFundFlowCaptureBatchRepository stockFundFlowCaptureBatchRepository;

    public StockFundFlowCaptureBatchDomainServiceImpl(StockFundFlowCaptureBatchRepository stockFundFlowCaptureBatchRepository) {
        this.stockFundFlowCaptureBatchRepository = stockFundFlowCaptureBatchRepository;
    }

    @Override
    public List<StockFundFlowCaptureBatchBO> list() {
        return stockFundFlowCaptureBatchRepository.list();
    }

    @Override
    public StockFundFlowCaptureBatchBO findById(Long id) {
        return stockFundFlowCaptureBatchRepository.findById(id);
    }

    @Override
    public Long create(StockFundFlowCaptureBatchBO value) {
        return stockFundFlowCaptureBatchRepository.create(value);
    }

    @Override
    public boolean update(StockFundFlowCaptureBatchBO value) {
        return stockFundFlowCaptureBatchRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return stockFundFlowCaptureBatchRepository.delete(id);
    }
}
