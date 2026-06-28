package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRepository;
import cn.cccs7.cynexus.market.domain.service.SectorDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorDomainServiceImpl implements SectorDomainService {

    private final SectorRepository sectorRepository;

    public SectorDomainServiceImpl(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @Override
    public List<SectorBO> list() {
        return sectorRepository.list();
    }

    @Override
    public SectorBO findById(Long id) {
        return sectorRepository.findById(id);
    }

    @Override
    public Long create(SectorBO value) {
        return sectorRepository.create(value);
    }

    @Override
    public boolean update(SectorBO value) {
        return sectorRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return sectorRepository.delete(id);
    }
}
