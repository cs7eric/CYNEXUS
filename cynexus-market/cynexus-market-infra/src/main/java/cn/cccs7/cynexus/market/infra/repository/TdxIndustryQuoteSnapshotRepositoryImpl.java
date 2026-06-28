package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.TdxIndustryQuoteSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.TdxIndustryQuoteSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.TdxIndustryQuoteSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TdxIndustryQuoteSnapshotRepositoryImpl implements TdxIndustryQuoteSnapshotRepository {

    private final TdxIndustryQuoteSnapshotBasicService tdxIndustryQuoteSnapshotBasicService;

    public TdxIndustryQuoteSnapshotRepositoryImpl(TdxIndustryQuoteSnapshotBasicService tdxIndustryQuoteSnapshotBasicService) {
        this.tdxIndustryQuoteSnapshotBasicService = tdxIndustryQuoteSnapshotBasicService;
    }

    @Override
    public List<TdxIndustryQuoteSnapshotBO> list() {
        return TdxIndustryQuoteSnapshotPOConverter.INSTANCE.toTargetList(tdxIndustryQuoteSnapshotBasicService.listAll());
    }

    @Override
    public TdxIndustryQuoteSnapshotBO findById(Long id) {
        return Optional.ofNullable(tdxIndustryQuoteSnapshotBasicService.getById(id))
                .map(TdxIndustryQuoteSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TdxIndustryQuoteSnapshotBO value) {
        return tdxIndustryQuoteSnapshotBasicService.create(TdxIndustryQuoteSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TdxIndustryQuoteSnapshotBO value) {
        return tdxIndustryQuoteSnapshotBasicService.update(TdxIndustryQuoteSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return tdxIndustryQuoteSnapshotBasicService.delete(id);
    }
}
