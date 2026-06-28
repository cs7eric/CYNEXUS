package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.OverviewDetailSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.OverviewDetailSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.OverviewDetailSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OverviewDetailSnapshotRepositoryImpl implements OverviewDetailSnapshotRepository {

    private final OverviewDetailSnapshotBasicService overviewDetailSnapshotBasicService;

    public OverviewDetailSnapshotRepositoryImpl(OverviewDetailSnapshotBasicService overviewDetailSnapshotBasicService) {
        this.overviewDetailSnapshotBasicService = overviewDetailSnapshotBasicService;
    }

    @Override
    public List<OverviewDetailSnapshotBO> list() {
        return OverviewDetailSnapshotPOConverter.INSTANCE.toTargetList(overviewDetailSnapshotBasicService.listAll());
    }

    @Override
    public OverviewDetailSnapshotBO findById(Long id) {
        return Optional.ofNullable(overviewDetailSnapshotBasicService.getById(id))
                .map(OverviewDetailSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(OverviewDetailSnapshotBO value) {
        return overviewDetailSnapshotBasicService.create(OverviewDetailSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(OverviewDetailSnapshotBO value) {
        return overviewDetailSnapshotBasicService.update(OverviewDetailSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return overviewDetailSnapshotBasicService.delete(id);
    }
}
