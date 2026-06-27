package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.domain.repository.TurnoverActivityDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.TurnoverActivityDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.TurnoverActivityDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TurnoverActivityDailyRepositoryImpl implements TurnoverActivityDailyRepository {

    private final TurnoverActivityDailyBasicService turnoverActivityDailyBasicService;

    public TurnoverActivityDailyRepositoryImpl(TurnoverActivityDailyBasicService turnoverActivityDailyBasicService) {
        this.turnoverActivityDailyBasicService = turnoverActivityDailyBasicService;
    }

    @Override
    public List<TurnoverActivityDailyBO> list() {
        return TurnoverActivityDailyPOConverter.INSTANCE.toTargetList(turnoverActivityDailyBasicService.listAll());
    }

    @Override
    public TurnoverActivityDailyBO findById(Long id) {
        return Optional.ofNullable(turnoverActivityDailyBasicService.getById(id))
                .map(TurnoverActivityDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TurnoverActivityDailyBO value) {
        return turnoverActivityDailyBasicService.create(TurnoverActivityDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TurnoverActivityDailyBO value) {
        return turnoverActivityDailyBasicService.update(TurnoverActivityDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return turnoverActivityDailyBasicService.delete(id);
    }
}
