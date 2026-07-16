package cn.cccs7.cynexus.market.domain.sentiment;

import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import java.util.List;

public interface TurnoverActivityDailyDomainService {

    List<TurnoverActivityDailyBO> list();

    TurnoverActivityDailyBO findById(Long id);

    Long create(TurnoverActivityDailyBO value);

    boolean update(TurnoverActivityDailyBO value);

    boolean delete(Long id);
}
