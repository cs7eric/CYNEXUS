package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import java.util.List;

public interface MaCountDailyDomainService {

    List<MaCountDailyBO> list();

    MaCountDailyBO findById(Long id);

    Long create(MaCountDailyBO value);

    boolean update(MaCountDailyBO value);

    boolean delete(Long id);
}
