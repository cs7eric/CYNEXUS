package cn.cccs7.cynexus.market.domain.stock;

import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import java.util.List;

public interface StockBasicDomainService {

    List<StockBasicBO> list();

    StockBasicBO findById(Long id);

    Long create(StockBasicBO value);

    boolean update(StockBasicBO value);

    boolean delete(Long id);
}
