package cn.cccs7.cynexus.market.domain.service.stock.impl;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;
import cn.cccs7.cynexus.market.domain.repository.StockInformationRepository;
import cn.cccs7.cynexus.market.domain.service.stock.StockInformationQueryDomainService;
import org.springframework.stereotype.Service;

@Service
public class StockInformationQueryDomainServiceImpl implements StockInformationQueryDomainService {

    private static final int MAX_LIMIT = 100;
    private final StockInformationRepository repository;

    public StockInformationQueryDomainServiceImpl(StockInformationRepository repository) {
        this.repository = repository;
    }

    @Override
    public StockInformationBO.Feed feed(String symbol, int limit) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol is required");
        }
        return repository.feed(symbol.trim().toUpperCase(), Math.max(1, Math.min(limit, MAX_LIMIT)));
    }
}
