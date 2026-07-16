package cn.cccs7.cynexus.market.domain.stock;

import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import java.util.List;

public interface StockSectorDomainService {

    List<StockSectorBO> list();

    StockSectorBO findById(Long id);

    Long create(StockSectorBO value);

    boolean update(StockSectorBO value);

    boolean delete(Long id);
}
