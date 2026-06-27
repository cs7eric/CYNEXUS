package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import java.util.List;

public interface SectorBreadthDailyDomainService {

    List<SectorBreadthDailyBO> list();

    SectorBreadthDailyBO findById(Long id);

    Long create(SectorBreadthDailyBO value);

    boolean update(SectorBreadthDailyBO value);

    boolean delete(Long id);
}
