package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.TdxSectorQuoteDetailSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.TdxSectorQuoteDetailSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.TdxSectorQuoteDetailSnapshotPOConverter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TdxSectorQuoteDetailSnapshotRepositoryImpl implements TdxSectorQuoteDetailSnapshotRepository {

    private final TdxSectorQuoteDetailSnapshotBasicService tdxSectorQuoteDetailSnapshotBasicService;

    public TdxSectorQuoteDetailSnapshotRepositoryImpl(TdxSectorQuoteDetailSnapshotBasicService tdxSectorQuoteDetailSnapshotBasicService) {
        this.tdxSectorQuoteDetailSnapshotBasicService = tdxSectorQuoteDetailSnapshotBasicService;
    }

    @Override
    public List<TdxSectorQuoteDetailSnapshotBO> list() {
        return TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE.toTargetList(tdxSectorQuoteDetailSnapshotBasicService.listAll());
    }

    @Override
    public Optional<TdxSectorQuoteDetailSnapshotBO> findLatestByCode6(String code6) {
        return tdxSectorQuoteDetailSnapshotBasicService.findLatestByCode6(code6)
                .map(TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<TdxSectorQuoteDetailSnapshotBO> findLatestByCode6OnOrBefore(String code6, LocalDate snapshotDate) {
        return tdxSectorQuoteDetailSnapshotBasicService.findLatestByCode6OnOrBefore(code6, snapshotDate)
                .map(TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public TdxSectorQuoteDetailSnapshotBO findById(Long id) {
        return Optional.ofNullable(tdxSectorQuoteDetailSnapshotBasicService.getById(id))
                .map(TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TdxSectorQuoteDetailSnapshotBO value) {
        return tdxSectorQuoteDetailSnapshotBasicService.create(TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TdxSectorQuoteDetailSnapshotBO value) {
        return tdxSectorQuoteDetailSnapshotBasicService.update(TdxSectorQuoteDetailSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return tdxSectorQuoteDetailSnapshotBasicService.delete(id);
    }
}
