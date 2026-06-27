package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import java.util.List;

public interface ProfitEffectDailyDomainService {

    List<ProfitEffectDailyBO> list();

    ProfitEffectDailyBO findById(Long id);

    Long create(ProfitEffectDailyBO value);

    boolean update(ProfitEffectDailyBO value);

    boolean delete(Long id);
}
