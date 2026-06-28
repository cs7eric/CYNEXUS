package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import java.util.List;

public interface StockSectorRepository {

    List<StockSectorBO> list();

    StockSectorBO findById(Long id);

    Long create(StockSectorBO value);

    boolean update(StockSectorBO value);

    boolean delete(Long id);
}
