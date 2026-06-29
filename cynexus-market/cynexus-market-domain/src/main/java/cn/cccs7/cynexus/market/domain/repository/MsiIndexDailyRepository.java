package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import java.util.List;
import java.util.Optional;

public interface MsiIndexDailyRepository {

    List<MsiIndexDailyBO> list();

    Optional<MsiIndexDailyBO> findLatest();

    Optional<MsiIndexDailyBO> findByTradeDate(java.time.LocalDate tradeDate);

    List<MsiIndexDailyBO> listByTradeDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
