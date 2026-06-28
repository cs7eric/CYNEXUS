package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import cn.cccs7.cynexus.market.domain.repository.TradeCalendarRepository;
import cn.cccs7.cynexus.market.infra.basic.service.TradeCalendarBasicService;
import cn.cccs7.cynexus.market.infra.convert.TradeCalendarPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TradeCalendarRepositoryImpl implements TradeCalendarRepository {

    private final TradeCalendarBasicService tradeCalendarBasicService;

    public TradeCalendarRepositoryImpl(TradeCalendarBasicService tradeCalendarBasicService) {
        this.tradeCalendarBasicService = tradeCalendarBasicService;
    }

    @Override
    public List<TradeCalendarBO> list() {
        return TradeCalendarPOConverter.INSTANCE.toTargetList(tradeCalendarBasicService.listAll());
    }

    @Override
    public TradeCalendarBO findById(Long id) {
        return Optional.ofNullable(tradeCalendarBasicService.getById(id))
                .map(TradeCalendarPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(TradeCalendarBO value) {
        return tradeCalendarBasicService.create(TradeCalendarPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean update(TradeCalendarBO value) {
        return tradeCalendarBasicService.update(TradeCalendarPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public boolean delete(Long id) {
        return tradeCalendarBasicService.delete(id);
    }
}
