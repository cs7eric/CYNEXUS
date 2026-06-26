package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseSectorDailySnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseSectorDailySnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketPulseSectorDailySnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketPulseSectorDailySnapshotRepositoryImpl implements MarketPulseSectorDailySnapshotRepository {

    private final MarketPulseSectorDailySnapshotBasicService basicService;

    public MarketPulseSectorDailySnapshotRepositoryImpl(MarketPulseSectorDailySnapshotBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MarketPulseSectorDailySnapshotBO> listAll() {
        return MarketPulseSectorDailySnapshotPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public MarketPulseSectorDailySnapshotBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(MarketPulseSectorDailySnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MarketPulseSectorDailySnapshotBO value) {
        return basicService.create(MarketPulseSectorDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MarketPulseSectorDailySnapshotBO value) {
        return basicService.update(MarketPulseSectorDailySnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
