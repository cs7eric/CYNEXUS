package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.HeatmapSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.HeatmapSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.HeatmapSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class HeatmapSnapshotRepositoryImpl implements HeatmapSnapshotRepository {

    private final HeatmapSnapshotBasicService heatmapSnapshotBasicService;

    public HeatmapSnapshotRepositoryImpl(HeatmapSnapshotBasicService heatmapSnapshotBasicService) {
        this.heatmapSnapshotBasicService = heatmapSnapshotBasicService;
    }

    @Override
    public List<HeatmapSnapshotBO> list() {
        return HeatmapSnapshotPOConverter.INSTANCE.toTargetList(heatmapSnapshotBasicService.listAll());
    }

    @Override
    public HeatmapSnapshotBO findById(Long id) {
        return Optional.ofNullable(heatmapSnapshotBasicService.getById(id))
                .map(HeatmapSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(HeatmapSnapshotBO value) {
        return heatmapSnapshotBasicService.create(HeatmapSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(HeatmapSnapshotBO value) {
        return heatmapSnapshotBasicService.update(HeatmapSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return heatmapSnapshotBasicService.delete(id);
    }
}
