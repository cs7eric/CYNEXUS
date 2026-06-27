package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import java.util.List;

public interface RiskAppetiteDailyRepository {

    List<RiskAppetiteDailyBO> list();

    RiskAppetiteDailyBO findById(Long id);

    Long create(RiskAppetiteDailyBO value);

    boolean update(RiskAppetiteDailyBO value);

    boolean delete(Long id);
}
