package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketLimitDailySnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketLimitDailySnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketLimitDailySnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketLimitDailySnapshotRepositoryImpl implements MarketLimitDailySnapshotRepository {

    private final MarketLimitDailySnapshotBasicService basicService;

    public MarketLimitDailySnapshotRepositoryImpl(MarketLimitDailySnapshotBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MarketLimitDailySnapshotBO> listAll() {
        return MarketLimitDailySnapshotPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public MarketLimitDailySnapshotBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(MarketLimitDailySnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MarketLimitDailySnapshotBO value) {
        return basicService.create(MarketLimitDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MarketLimitDailySnapshotBO value) {
        return basicService.update(MarketLimitDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
