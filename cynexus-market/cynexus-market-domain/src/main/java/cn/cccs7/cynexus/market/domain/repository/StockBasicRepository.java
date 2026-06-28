package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import java.util.List;

public interface StockBasicRepository {

    List<StockBasicBO> list();

    StockBasicBO findById(Long id);

    Long create(StockBasicBO value);

    boolean update(StockBasicBO value);

    boolean delete(Long id);
}
