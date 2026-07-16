package cn.cccs7.cynexus.market.domain.calendar;

import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import java.util.List;

public interface TradeCalendarDomainService {

    List<TradeCalendarBO> list();

    TradeCalendarBO findById(Long id);

    Long create(TradeCalendarBO value);

    boolean update(TradeCalendarBO value);

    boolean delete(Long id);
}
