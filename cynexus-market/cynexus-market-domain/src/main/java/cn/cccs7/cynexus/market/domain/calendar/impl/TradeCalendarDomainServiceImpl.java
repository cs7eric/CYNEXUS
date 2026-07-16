package cn.cccs7.cynexus.market.domain.calendar.impl;

import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import cn.cccs7.cynexus.market.domain.repository.TradeCalendarRepository;
import cn.cccs7.cynexus.market.domain.calendar.TradeCalendarDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TradeCalendarDomainServiceImpl implements TradeCalendarDomainService {

    private final TradeCalendarRepository tradeCalendarRepository;

    public TradeCalendarDomainServiceImpl(TradeCalendarRepository tradeCalendarRepository) {
        this.tradeCalendarRepository = tradeCalendarRepository;
    }

    @Override
    public List<TradeCalendarBO> list() {
        return tradeCalendarRepository.list();
    }

    @Override
    public TradeCalendarBO findById(Long id) {
        return tradeCalendarRepository.findById(id);
    }

    @Override
    public Long create(TradeCalendarBO value) {
        return tradeCalendarRepository.create(value);
    }

    @Override
    public boolean update(TradeCalendarBO value) {
        return tradeCalendarRepository.update(value);
    }

    @Override
    public boolean delete(Long id) {
        return tradeCalendarRepository.delete(id);
    }
}
