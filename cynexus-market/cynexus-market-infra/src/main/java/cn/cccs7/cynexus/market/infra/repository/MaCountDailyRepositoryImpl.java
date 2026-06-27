package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.repository.MaCountDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MaCountDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.MaCountDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MaCountDailyRepositoryImpl implements MaCountDailyRepository {

    private final MaCountDailyBasicService maCountDailyBasicService;

    public MaCountDailyRepositoryImpl(MaCountDailyBasicService maCountDailyBasicService) {
        this.maCountDailyBasicService = maCountDailyBasicService;
    }

    @Override
    public List<MaCountDailyBO> list() {
        return MaCountDailyPOConverter.INSTANCE.toTargetList(maCountDailyBasicService.listAll());
    }

    @Override
    public MaCountDailyBO findById(Long id) {
        return Optional.ofNullable(maCountDailyBasicService.getById(id))
                .map(MaCountDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(MaCountDailyBO value) {
        return maCountDailyBasicService.create(MaCountDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(MaCountDailyBO value) {
        return maCountDailyBasicService.update(MaCountDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return maCountDailyBasicService.delete(id);
    }
}
