package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import java.util.List;

public interface SectorBreadthDailyRepository {

    List<SectorBreadthDailyBO> list();

    SectorBreadthDailyBO findById(Long id);

    Long create(SectorBreadthDailyBO value);

    boolean update(SectorBreadthDailyBO value);

    boolean delete(Long id);
}
