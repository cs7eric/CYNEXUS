package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.domain.repository.ProfitEffectDailyRepository;
import cn.cccs7.cynexus.market.domain.service.ProfitEffectDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfitEffectDailyDomainServiceImpl implements ProfitEffectDailyDomainService {

    private final ProfitEffectDailyRepository profitEffectDailyRepository;

    public ProfitEffectDailyDomainServiceImpl(ProfitEffectDailyRepository profitEffectDailyRepository) {
        this.profitEffectDailyRepository = profitEffectDailyRepository;
    }

    @Override
    public List<ProfitEffectDailyBO> list() {
        return profitEffectDailyRepository.list();
    }

    @Override
    public ProfitEffectDailyBO findById(Long id) {
        return profitEffectDailyRepository.findById(id);
    }

    @Override
    public Long create(ProfitEffectDailyBO value) {
        return profitEffectDailyRepository.create(value);
    }

    @Override
    public boolean update(ProfitEffectDailyBO value) {
        return profitEffectDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return profitEffectDailyRepository.delete(id);
    }
}
