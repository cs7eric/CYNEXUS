package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import java.util.List;

public interface SectorRepository {

    List<SectorBO> list();

    SectorBO findById(Long id);

    Long create(SectorBO value);

    boolean update(SectorBO value);

    boolean delete(Long id);
}
