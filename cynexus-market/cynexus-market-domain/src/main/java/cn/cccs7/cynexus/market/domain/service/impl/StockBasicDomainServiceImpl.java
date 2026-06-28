package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import cn.cccs7.cynexus.market.domain.repository.StockBasicRepository;
import cn.cccs7.cynexus.market.domain.service.StockBasicDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockBasicDomainServiceImpl implements StockBasicDomainService {

    private final StockBasicRepository stockBasicRepository;

    public StockBasicDomainServiceImpl(StockBasicRepository stockBasicRepository) {
        this.stockBasicRepository = stockBasicRepository;
    }

    @Override
    public List<StockBasicBO> list() {
        return stockBasicRepository.list();
    }

    @Override
    public StockBasicBO findById(Long id) {
        return stockBasicRepository.findById(id);
    }

    @Override
    public Long create(StockBasicBO value) {
        return stockBasicRepository.create(value);
    }

    @Override
    public boolean update(StockBasicBO value) {
        return stockBasicRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return stockBasicRepository.delete(id);
    }
}
