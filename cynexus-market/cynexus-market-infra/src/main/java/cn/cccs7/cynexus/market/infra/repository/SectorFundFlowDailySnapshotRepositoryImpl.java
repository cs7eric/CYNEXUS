package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.SectorFundFlowDailySnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowDailySnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.SectorFundFlowDailySnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SectorFundFlowDailySnapshotRepositoryImpl implements SectorFundFlowDailySnapshotRepository {

    private final SectorFundFlowDailySnapshotBasicService basicService;

    public SectorFundFlowDailySnapshotRepositoryImpl(SectorFundFlowDailySnapshotBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<SectorFundFlowDailySnapshotBO> listAll() {
        return SectorFundFlowDailySnapshotPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public SectorFundFlowDailySnapshotBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(SectorFundFlowDailySnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SectorFundFlowDailySnapshotBO value) {
        return basicService.create(SectorFundFlowDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SectorFundFlowDailySnapshotBO value) {
        return basicService.update(SectorFundFlowDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
