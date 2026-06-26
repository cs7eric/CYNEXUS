package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseCaptureBatchRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseCaptureBatchBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarketPulseCaptureBatchPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketPulseCaptureBatchRepositoryImpl implements MarketPulseCaptureBatchRepository {

    private final MarketPulseCaptureBatchBasicService basicService;

    public MarketPulseCaptureBatchRepositoryImpl(MarketPulseCaptureBatchBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MarketPulseCaptureBatchBO> listAll() {
        return MarketPulseCaptureBatchPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public MarketPulseCaptureBatchBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(MarketPulseCaptureBatchPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MarketPulseCaptureBatchBO value) {
        return basicService.create(MarketPulseCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MarketPulseCaptureBatchBO value) {
        return basicService.update(MarketPulseCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
