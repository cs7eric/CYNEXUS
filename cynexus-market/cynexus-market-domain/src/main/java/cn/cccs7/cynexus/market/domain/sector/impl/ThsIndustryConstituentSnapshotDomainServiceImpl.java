package cn.cccs7.cynexus.market.domain.sector.impl;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import cn.cccs7.cynexus.market.domain.sector.ThsIndustryConstituentSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentSnapshotDomainServiceImpl implements ThsIndustryConstituentSnapshotDomainService {

    private final ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository;

    public ThsIndustryConstituentSnapshotDomainServiceImpl(ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository) {
        this.thsIndustryConstituentSnapshotRepository = thsIndustryConstituentSnapshotRepository;
    }

    @Override
    public List<ThsIndustryConstituentSnapshotBO> list() {
        return thsIndustryConstituentSnapshotRepository.list();
    }

    @Override
    public ThsIndustryConstituentSnapshotBO findById(Long id) {
        return thsIndustryConstituentSnapshotRepository.findById(id);
    }

    @Override
    public Long create(ThsIndustryConstituentSnapshotBO value) {
        return thsIndustryConstituentSnapshotRepository.create(value);
    }

    @Override
    public boolean update(ThsIndustryConstituentSnapshotBO value) {
        return thsIndustryConstituentSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return thsIndustryConstituentSnapshotRepository.delete(id);
    }
}
