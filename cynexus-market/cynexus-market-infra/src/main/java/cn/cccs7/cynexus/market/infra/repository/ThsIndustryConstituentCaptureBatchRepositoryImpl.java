package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentCaptureBatchRepository;
import cn.cccs7.cynexus.market.infra.basic.service.ThsIndustryConstituentCaptureBatchBasicService;
import cn.cccs7.cynexus.market.infra.convert.ThsIndustryConstituentCaptureBatchPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ThsIndustryConstituentCaptureBatchRepositoryImpl implements ThsIndustryConstituentCaptureBatchRepository {

    private final ThsIndustryConstituentCaptureBatchBasicService thsIndustryConstituentCaptureBatchBasicService;

    public ThsIndustryConstituentCaptureBatchRepositoryImpl(ThsIndustryConstituentCaptureBatchBasicService thsIndustryConstituentCaptureBatchBasicService) {
        this.thsIndustryConstituentCaptureBatchBasicService = thsIndustryConstituentCaptureBatchBasicService;
    }

    @Override
    public List<ThsIndustryConstituentCaptureBatchBO> list() {
        return ThsIndustryConstituentCaptureBatchPOConverter.INSTANCE.toTargetList(thsIndustryConstituentCaptureBatchBasicService.listAll());
    }

    @Override
    public ThsIndustryConstituentCaptureBatchBO findById(Long id) {
        return Optional.ofNullable(thsIndustryConstituentCaptureBatchBasicService.getById(id))
                .map(ThsIndustryConstituentCaptureBatchPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ThsIndustryConstituentCaptureBatchBO value) {
        return thsIndustryConstituentCaptureBatchBasicService.create(ThsIndustryConstituentCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(ThsIndustryConstituentCaptureBatchBO value) {
        return thsIndustryConstituentCaptureBatchBasicService.update(ThsIndustryConstituentCaptureBatchPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return thsIndustryConstituentCaptureBatchBasicService.delete(id);
    }
}
