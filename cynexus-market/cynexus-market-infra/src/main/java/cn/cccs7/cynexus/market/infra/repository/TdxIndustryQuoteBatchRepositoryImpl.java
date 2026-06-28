package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import cn.cccs7.cynexus.market.domain.repository.TdxIndustryQuoteBatchRepository;
import cn.cccs7.cynexus.market.infra.basic.service.TdxIndustryQuoteBatchBasicService;
import cn.cccs7.cynexus.market.infra.convert.TdxIndustryQuoteBatchPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TdxIndustryQuoteBatchRepositoryImpl implements TdxIndustryQuoteBatchRepository {

    private final TdxIndustryQuoteBatchBasicService tdxIndustryQuoteBatchBasicService;

    public TdxIndustryQuoteBatchRepositoryImpl(TdxIndustryQuoteBatchBasicService tdxIndustryQuoteBatchBasicService) {
        this.tdxIndustryQuoteBatchBasicService = tdxIndustryQuoteBatchBasicService;
    }

    @Override
    public List<TdxIndustryQuoteBatchBO> list() {
        return TdxIndustryQuoteBatchPOConverter.INSTANCE.toTargetList(tdxIndustryQuoteBatchBasicService.listAll());
    }

    @Override
    public TdxIndustryQuoteBatchBO findById(Long id) {
        return Optional.ofNullable(tdxIndustryQuoteBatchBasicService.getById(id))
                .map(TdxIndustryQuoteBatchPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TdxIndustryQuoteBatchBO value) {
        return tdxIndustryQuoteBatchBasicService.create(TdxIndustryQuoteBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TdxIndustryQuoteBatchBO value) {
        return tdxIndustryQuoteBatchBasicService.update(TdxIndustryQuoteBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return tdxIndustryQuoteBatchBasicService.delete(id);
    }
}
