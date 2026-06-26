package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.SectorFundFlowCaptureBatchRepository;
import cn.cccs7.cynexus.market.infra.basic.service.SectorFundFlowCaptureBatchBasicService;
import cn.cccs7.cynexus.market.infra.convert.SectorFundFlowCaptureBatchPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SectorFundFlowCaptureBatchRepositoryImpl implements SectorFundFlowCaptureBatchRepository {

    private final SectorFundFlowCaptureBatchBasicService basicService;

    public SectorFundFlowCaptureBatchRepositoryImpl(SectorFundFlowCaptureBatchBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<SectorFundFlowCaptureBatchBO> listAll() {
        return SectorFundFlowCaptureBatchPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public SectorFundFlowCaptureBatchBO findById(Long id) {
        return Optional.ofNullable(basicService.getById(id))
                .map(SectorFundFlowCaptureBatchPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(SectorFundFlowCaptureBatchBO value) {
        return basicService.create(SectorFundFlowCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(SectorFundFlowCaptureBatchBO value) {
        return basicService.update(SectorFundFlowCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return basicService.delete(id);
    }
}
