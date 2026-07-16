package cn.cccs7.cynexus.market.domain.pulse.impl;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.HeatmapSnapshotRepository;
import cn.cccs7.cynexus.market.domain.pulse.HeatmapSnapshotDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HeatmapSnapshotDomainServiceImpl implements HeatmapSnapshotDomainService {

    private final HeatmapSnapshotRepository heatmapSnapshotRepository;

    public HeatmapSnapshotDomainServiceImpl(HeatmapSnapshotRepository heatmapSnapshotRepository) {
        this.heatmapSnapshotRepository = heatmapSnapshotRepository;
    }

    @Override
    public List<HeatmapSnapshotBO> list() {
        return heatmapSnapshotRepository.list();
    }

    @Override
    public HeatmapSnapshotBO findById(Long id) {
        return heatmapSnapshotRepository.findById(id);
    }

    @Override
    public Long create(HeatmapSnapshotBO value) {
        return heatmapSnapshotRepository.create(value);
    }

    @Override
    public boolean update(HeatmapSnapshotBO value) {
        return heatmapSnapshotRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return heatmapSnapshotRepository.delete(id);
    }
}
