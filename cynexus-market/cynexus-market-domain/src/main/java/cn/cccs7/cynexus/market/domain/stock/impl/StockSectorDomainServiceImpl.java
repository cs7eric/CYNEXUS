package cn.cccs7.cynexus.market.domain.stock.impl;

import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import cn.cccs7.cynexus.market.domain.repository.StockSectorRepository;
import cn.cccs7.cynexus.market.domain.stock.StockSectorDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockSectorDomainServiceImpl implements StockSectorDomainService {

    private final StockSectorRepository stockSectorRepository;

    public StockSectorDomainServiceImpl(StockSectorRepository stockSectorRepository) {
        this.stockSectorRepository = stockSectorRepository;
    }

    @Override
    public List<StockSectorBO> list() {
        return stockSectorRepository.list();
    }

    @Override
    public StockSectorBO findById(Long id) {
        return stockSectorRepository.findById(id);
    }

    @Override
    public Long create(StockSectorBO value) {
        return stockSectorRepository.create(value);
    }

    @Override
    public boolean update(StockSectorBO value) {
        return stockSectorRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return stockSectorRepository.delete(id);
    }
}
