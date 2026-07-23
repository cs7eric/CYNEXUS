package cn.cccs7.cynexus.market.domain.service.sector.impl;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRankRepository;
import cn.cccs7.cynexus.market.domain.service.sector.SectorRankQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorRankQueryDomainServiceImpl implements SectorRankQueryDomainService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private final SectorRankRepository repository;

    public SectorRankQueryDomainServiceImpl(SectorRankRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SectorRankBO> getSectorRank(String tradeDate, Integer topN) {
        int limit = topN == null || topN <= 0 ? DEFAULT_LIMIT : Math.min(topN, MAX_LIMIT);
        return repository.findByTradeDate(tradeDate, limit);
    }
}
