package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import cn.cccs7.cynexus.market.domain.repository.TdxIndustryQuoteBatchRepository;
import cn.cccs7.cynexus.market.domain.service.TdxIndustryQuoteBatchDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteBatchDomainServiceImpl implements TdxIndustryQuoteBatchDomainService {

    private final TdxIndustryQuoteBatchRepository tdxIndustryQuoteBatchRepository;

    public TdxIndustryQuoteBatchDomainServiceImpl(TdxIndustryQuoteBatchRepository tdxIndustryQuoteBatchRepository) {
        this.tdxIndustryQuoteBatchRepository = tdxIndustryQuoteBatchRepository;
    }

    @Override
    public List<TdxIndustryQuoteBatchBO> list() {
        return tdxIndustryQuoteBatchRepository.list();
    }

    @Override
    public TdxIndustryQuoteBatchBO findById(Long id) {
        return tdxIndustryQuoteBatchRepository.findById(id);
    }

    @Override
    public Long create(TdxIndustryQuoteBatchBO value) {
        return tdxIndustryQuoteBatchRepository.create(value);
    }

    @Override
    public boolean update(TdxIndustryQuoteBatchBO value) {
        return tdxIndustryQuoteBatchRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return tdxIndustryQuoteBatchRepository.delete(id);
    }
}
