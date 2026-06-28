package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SectorBasicService;
import cn.cccs7.cynexus.market.infra.convert.SectorPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SectorRepositoryImpl implements SectorRepository {

    private final SectorBasicService sectorBasicService;

    public SectorRepositoryImpl(SectorBasicService sectorBasicService) {
        this.sectorBasicService = sectorBasicService;
    }

    @Override
    public List<SectorBO> list() {
        return SectorPOConverter.INSTANCE.toTargetList(sectorBasicService.listAll());
    }

    @Override
    public SectorBO findById(Long id) {
        return Optional.ofNullable(sectorBasicService.getById(id))
                .map(SectorPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SectorBO value) {
        return sectorBasicService.create(SectorPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SectorBO value) {
        return sectorBasicService.update(SectorPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return sectorBasicService.delete(id);
    }
}
