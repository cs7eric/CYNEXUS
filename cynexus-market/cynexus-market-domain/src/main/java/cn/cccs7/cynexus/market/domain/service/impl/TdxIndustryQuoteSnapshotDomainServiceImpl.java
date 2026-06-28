package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.TdxIndustryQuoteSnapshotRepository;
import cn.cccs7.cynexus.market.domain.service.TdxIndustryQuoteSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxIndustryQuoteSnapshotDomainServiceImpl implements TdxIndustryQuoteSnapshotDomainService {

    private final TdxIndustryQuoteSnapshotRepository tdxIndustryQuoteSnapshotRepository;

    public TdxIndustryQuoteSnapshotDomainServiceImpl(TdxIndustryQuoteSnapshotRepository tdxIndustryQuoteSnapshotRepository) {
        this.tdxIndustryQuoteSnapshotRepository = tdxIndustryQuoteSnapshotRepository;
    }

    @Override
    public List<TdxIndustryQuoteSnapshotBO> list() {
        return tdxIndustryQuoteSnapshotRepository.list();
    }

    @Override
    public TdxIndustryQuoteSnapshotBO findById(Long id) {
        return tdxIndustryQuoteSnapshotRepository.findById(id);
    }

    @Override
    public Long create(TdxIndustryQuoteSnapshotBO value) {
        return tdxIndustryQuoteSnapshotRepository.create(value);
    }

    @Override
    public boolean update(TdxIndustryQuoteSnapshotBO value) {
        return tdxIndustryQuoteSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return tdxIndustryQuoteSnapshotRepository.delete(id);
    }
}
