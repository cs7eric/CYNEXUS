package cn.cccs7.cynexus.market.domain.sector.impl;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentCaptureBatchRepository;
import cn.cccs7.cynexus.market.domain.sector.ThsIndustryConstituentCaptureBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThsIndustryConstituentCaptureBatchDomainServiceImpl implements ThsIndustryConstituentCaptureBatchDomainService {

    private final ThsIndustryConstituentCaptureBatchRepository thsIndustryConstituentCaptureBatchRepository;

    public ThsIndustryConstituentCaptureBatchDomainServiceImpl(ThsIndustryConstituentCaptureBatchRepository thsIndustryConstituentCaptureBatchRepository) {
        this.thsIndustryConstituentCaptureBatchRepository = thsIndustryConstituentCaptureBatchRepository;
    }

    @Override
    public List<ThsIndustryConstituentCaptureBatchBO> list() {
        return thsIndustryConstituentCaptureBatchRepository.list();
    }

    @Override
    public ThsIndustryConstituentCaptureBatchBO findById(Long id) {
        return thsIndustryConstituentCaptureBatchRepository.findById(id);
    }

    @Override
    public Long create(ThsIndustryConstituentCaptureBatchBO value) {
        return thsIndustryConstituentCaptureBatchRepository.create(value);
    }

    @Override
    public boolean update(ThsIndustryConstituentCaptureBatchBO value) {
        return thsIndustryConstituentCaptureBatchRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return thsIndustryConstituentCaptureBatchRepository.delete(id);
    }
}
