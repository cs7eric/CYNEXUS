package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import java.util.List;

public interface SectorDomainService {

    List<SectorBO> list();

    SectorBO findById(Long id);

    Long create(SectorBO value);

    boolean update(SectorBO value);

    boolean delete(Long id);
}
