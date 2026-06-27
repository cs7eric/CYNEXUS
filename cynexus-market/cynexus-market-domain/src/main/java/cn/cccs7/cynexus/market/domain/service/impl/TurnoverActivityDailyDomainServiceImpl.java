package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.domain.repository.TurnoverActivityDailyRepository;
import cn.cccs7.cynexus.market.domain.service.TurnoverActivityDailyDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TurnoverActivityDailyDomainServiceImpl implements TurnoverActivityDailyDomainService {

    private final TurnoverActivityDailyRepository turnoverActivityDailyRepository;

    public TurnoverActivityDailyDomainServiceImpl(TurnoverActivityDailyRepository turnoverActivityDailyRepository) {
        this.turnoverActivityDailyRepository = turnoverActivityDailyRepository;
    }

    @Override
    public List<TurnoverActivityDailyBO> list() {
        return turnoverActivityDailyRepository.list();
    }

    @Override
    public TurnoverActivityDailyBO findById(Long id) {
        return turnoverActivityDailyRepository.findById(id);
    }

    @Override
    public Long create(TurnoverActivityDailyBO value) {
        return turnoverActivityDailyRepository.create(value);
    }

    @Override
    public boolean update(TurnoverActivityDailyBO value) {
        return turnoverActivityDailyRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return turnoverActivityDailyRepository.delete(id);
    }
}
