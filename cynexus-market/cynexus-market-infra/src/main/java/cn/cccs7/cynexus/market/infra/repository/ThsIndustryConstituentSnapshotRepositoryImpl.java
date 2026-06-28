package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import cn.cccs7.cynexus.market.infra.basic.service.ThsIndustryConstituentSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.convert.ThsIndustryConstituentSnapshotPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ThsIndustryConstituentSnapshotRepositoryImpl implements ThsIndustryConstituentSnapshotRepository {

    private final ThsIndustryConstituentSnapshotBasicService thsIndustryConstituentSnapshotBasicService;

    public ThsIndustryConstituentSnapshotRepositoryImpl(ThsIndustryConstituentSnapshotBasicService thsIndustryConstituentSnapshotBasicService) {
        this.thsIndustryConstituentSnapshotBasicService = thsIndustryConstituentSnapshotBasicService;
    }

    @Override
    public List<ThsIndustryConstituentSnapshotBO> list() {
        return ThsIndustryConstituentSnapshotPOConverter.INSTANCE.toTargetList(thsIndustryConstituentSnapshotBasicService.listAll());
    }

    @Override
    public ThsIndustryConstituentSnapshotBO findById(Long id) {
        return Optional.ofNullable(thsIndustryConstituentSnapshotBasicService.getById(id))
                .map(ThsIndustryConstituentSnapshotPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ThsIndustryConstituentSnapshotBO value) {
        return thsIndustryConstituentSnapshotBasicService.create(ThsIndustryConstituentSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(ThsIndustryConstituentSnapshotBO value) {
        return thsIndustryConstituentSnapshotBasicService.update(ThsIndustryConstituentSnapshotPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return thsIndustryConstituentSnapshotBasicService.delete(id);
    }
}
