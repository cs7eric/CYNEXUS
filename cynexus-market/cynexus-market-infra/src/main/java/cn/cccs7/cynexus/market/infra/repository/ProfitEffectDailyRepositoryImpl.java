package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.domain.repository.ProfitEffectDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.ProfitEffectDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.ProfitEffectDailyPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProfitEffectDailyRepositoryImpl implements ProfitEffectDailyRepository {

    private final ProfitEffectDailyBasicService profitEffectDailyBasicService;

    public ProfitEffectDailyRepositoryImpl(ProfitEffectDailyBasicService profitEffectDailyBasicService) {
        this.profitEffectDailyBasicService = profitEffectDailyBasicService;
    }

    @Override
    public List<ProfitEffectDailyBO> list() {
        return ProfitEffectDailyPOConverter.INSTANCE.toTargetList(profitEffectDailyBasicService.listAll());
    }

    @Override
    public ProfitEffectDailyBO findById(Long id) {
        return Optional.ofNullable(profitEffectDailyBasicService.getById(id))
                .map(ProfitEffectDailyPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(ProfitEffectDailyBO value) {
        return profitEffectDailyBasicService.create(ProfitEffectDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(ProfitEffectDailyBO value) {
        return profitEffectDailyBasicService.update(ProfitEffectDailyPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return profitEffectDailyBasicService.delete(id);
    }
}
