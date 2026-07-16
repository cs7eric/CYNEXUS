package cn.cccs7.cynexus.market.domain.sentiment.impl;

import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import cn.cccs7.cynexus.market.domain.repository.VolatilityDailyRepository;
import cn.cccs7.cynexus.market.domain.sentiment.VolatilityDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VolatilityDailyDomainServiceImpl implements VolatilityDailyDomainService {

    private final VolatilityDailyRepository volatilityDailyRepository;

    public VolatilityDailyDomainServiceImpl(VolatilityDailyRepository volatilityDailyRepository) {
        this.volatilityDailyRepository = volatilityDailyRepository;
    }

    @Override
    public List<VolatilityDailyBO> list() {
        return volatilityDailyRepository.list();
    }

    @Override
    public VolatilityDailyBO findById(Long id) {
        return volatilityDailyRepository.findById(id);
    }

    @Override
    public Long create(VolatilityDailyBO value) {
        return volatilityDailyRepository.create(value);
    }

    @Override
    public boolean update(VolatilityDailyBO value) {
        return volatilityDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return volatilityDailyRepository.delete(id);
    }
}
