package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.domain.repository.SectorBreadthDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SectorBreadthDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.SectorBreadthDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SectorBreadthDailyRepositoryImpl implements SectorBreadthDailyRepository {

    private final SectorBreadthDailyBasicService sectorBreadthDailyBasicService;

    public SectorBreadthDailyRepositoryImpl(SectorBreadthDailyBasicService sectorBreadthDailyBasicService) {
        this.sectorBreadthDailyBasicService = sectorBreadthDailyBasicService;
    }

    @Override
    public List<SectorBreadthDailyBO> list() {
        return SectorBreadthDailyPOConverter.INSTANCE.toTargetList(sectorBreadthDailyBasicService.listAll());
    }

    @Override
    public SectorBreadthDailyBO findById(Long id) {
        return Optional.ofNullable(sectorBreadthDailyBasicService.getById(id))
                .map(SectorBreadthDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SectorBreadthDailyBO value) {
        return sectorBreadthDailyBasicService.create(SectorBreadthDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SectorBreadthDailyBO value) {
        return sectorBreadthDailyBasicService.update(SectorBreadthDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return sectorBreadthDailyBasicService.delete(id);
    }
}
