package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.domain.repository.MsiIndexDailyRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MsiIndexDailyBasicService;
import cn.cccs7.cynexus.market.infra.convert.MsiIndexDailyPOConverter;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MsiIndexDailyRepositoryAdapter implements MsiIndexDailyRepository {

    private final MsiIndexDailyBasicService basicService;

    public MsiIndexDailyRepositoryAdapter(MsiIndexDailyBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<MsiIndexDailyBO> list() {
        return MsiIndexDailyPOConverter.INSTANCE.toTargetList(basicService.listAll());
    }

    @Override
    public Optional<MsiIndexDailyBO> findLatest() {
        return basicService.listAll().stream()
                .filter(item -> item.getTradeDate() != null)
                .max(Comparator.comparing(MsiIndexDailyPOConverter.INSTANCE::toTarget, Comparator.comparing(
                        item -> item.getTradeDate())))
                .map(MsiIndexDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MsiIndexDailyBO> findByTradeDate(LocalDate tradeDate) {
        return basicService.listAll().stream()
                .filter(item -> tradeDate.equals(item.getTradeDate()))
                .findFirst()
                .map(MsiIndexDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MsiIndexDailyBO> listByTradeDateRange(LocalDate startDate, LocalDate endDate) {
        return basicService.listAll().stream()
                .filter(item -> item.getTradeDate() != null
                        && !item.getTradeDate().isBefore(startDate)
                        && !item.getTradeDate().isAfter(endDate))
                .map(MsiIndexDailyPOConverter.INSTANCE::toTarget)
                .toList();
    }
}
