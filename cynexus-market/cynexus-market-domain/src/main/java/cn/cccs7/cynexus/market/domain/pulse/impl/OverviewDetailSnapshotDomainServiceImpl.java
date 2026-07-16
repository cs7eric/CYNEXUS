package cn.cccs7.cynexus.market.domain.pulse.impl;

import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.OverviewDetailSnapshotRepository;
import cn.cccs7.cynexus.market.domain.pulse.OverviewDetailSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OverviewDetailSnapshotDomainServiceImpl implements OverviewDetailSnapshotDomainService {

    private final OverviewDetailSnapshotRepository overviewDetailSnapshotRepository;

    public OverviewDetailSnapshotDomainServiceImpl(OverviewDetailSnapshotRepository overviewDetailSnapshotRepository) {
        this.overviewDetailSnapshotRepository = overviewDetailSnapshotRepository;
    }

    @Override
    public List<OverviewDetailSnapshotBO> list() {
        return overviewDetailSnapshotRepository.list();
    }

    @Override
    public OverviewDetailSnapshotBO findById(Long id) {
        return overviewDetailSnapshotRepository.findById(id);
    }

    @Override
    public Long create(OverviewDetailSnapshotBO value) {
        return overviewDetailSnapshotRepository.create(value);
    }

    @Override
    public boolean update(OverviewDetailSnapshotBO value) {
        return overviewDetailSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return overviewDetailSnapshotRepository.delete(id);
    }
}
