package cn.cccs7.cynexus.market.domain.sentiment.impl;

import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.domain.repository.SectorBreadthDailyRepository;
import cn.cccs7.cynexus.market.domain.sentiment.SectorBreadthDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorBreadthDailyDomainServiceImpl implements SectorBreadthDailyDomainService {

    private final SectorBreadthDailyRepository sectorBreadthDailyRepository;

    public SectorBreadthDailyDomainServiceImpl(SectorBreadthDailyRepository sectorBreadthDailyRepository) {
        this.sectorBreadthDailyRepository = sectorBreadthDailyRepository;
    }

    @Override
    public List<SectorBreadthDailyBO> list() {
        return sectorBreadthDailyRepository.list();
    }

    @Override
    public SectorBreadthDailyBO findById(Long id) {
        return sectorBreadthDailyRepository.findById(id);
    }

    @Override
    public Long create(SectorBreadthDailyBO value) {
        return sectorBreadthDailyRepository.create(value);
    }

    @Override
    public boolean update(SectorBreadthDailyBO value) {
        return sectorBreadthDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return sectorBreadthDailyRepository.delete(id);
    }
}
