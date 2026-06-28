package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import java.util.List;

public interface StockFundFlowCaptureBatchRepository {

    List<StockFundFlowCaptureBatchBO> list();

    StockFundFlowCaptureBatchBO findById(Long id);

    Long create(StockFundFlowCaptureBatchBO value);

    boolean update(StockFundFlowCaptureBatchBO value);

    boolean delete(Long id);
}
