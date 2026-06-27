package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import cn.cccs7.cynexus.market.domain.repository.VolatilityDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.VolatilityDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.VolatilityDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class VolatilityDailyRepositoryImpl implements VolatilityDailyRepository {

    private final VolatilityDailyBasicService volatilityDailyBasicService;

    public VolatilityDailyRepositoryImpl(VolatilityDailyBasicService volatilityDailyBasicService) {
        this.volatilityDailyBasicService = volatilityDailyBasicService;
    }

    @Override
    public List<VolatilityDailyBO> list() {
        return VolatilityDailyPOConverter.INSTANCE.toTargetList(volatilityDailyBasicService.listAll());
    }

    @Override
    public VolatilityDailyBO findById(Long id) {
        return Optional.ofNullable(volatilityDailyBasicService.getById(id))
                .map(VolatilityDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(VolatilityDailyBO value) {
        return volatilityDailyBasicService.create(VolatilityDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(VolatilityDailyBO value) {
        return volatilityDailyBasicService.update(VolatilityDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return volatilityDailyBasicService.delete(id);
    }
}
