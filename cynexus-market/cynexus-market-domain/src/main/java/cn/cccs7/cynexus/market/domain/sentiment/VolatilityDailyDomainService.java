package cn.cccs7.cynexus.market.domain.sentiment;

import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import java.util.List;

public interface VolatilityDailyDomainService {

    List<VolatilityDailyBO> list();

    VolatilityDailyBO findById(Long id);

    Long create(VolatilityDailyBO value);

    boolean update(VolatilityDailyBO value);

    boolean delete(Long id);
}
