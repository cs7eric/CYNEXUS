package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.TdxSectorQuoteDetailSnapshotRepository;
import cn.cccs7.cynexus.market.domain.service.TdxSectorQuoteDetailSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TdxSectorQuoteDetailSnapshotDomainServiceImpl implements TdxSectorQuoteDetailSnapshotDomainService {

    private final TdxSectorQuoteDetailSnapshotRepository tdxSectorQuoteDetailSnapshotRepository;

    public TdxSectorQuoteDetailSnapshotDomainServiceImpl(TdxSectorQuoteDetailSnapshotRepository tdxSectorQuoteDetailSnapshotRepository) {
        this.tdxSectorQuoteDetailSnapshotRepository = tdxSectorQuoteDetailSnapshotRepository;
    }

    @Override
    public List<TdxSectorQuoteDetailSnapshotBO> list() {
        return tdxSectorQuoteDetailSnapshotRepository.list();
    }

    @Override
    public TdxSectorQuoteDetailSnapshotBO findById(Long id) {
        return tdxSectorQuoteDetailSnapshotRepository.findById(id);
    }

    @Override
    public Long create(TdxSectorQuoteDetailSnapshotBO value) {
        return tdxSectorQuoteDetailSnapshotRepository.create(value);
    }

    @Override
    public boolean update(TdxSectorQuoteDetailSnapshotBO value) {
        return tdxSectorQuoteDetailSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return tdxSectorQuoteDetailSnapshotRepository.delete(id);
    }
}
