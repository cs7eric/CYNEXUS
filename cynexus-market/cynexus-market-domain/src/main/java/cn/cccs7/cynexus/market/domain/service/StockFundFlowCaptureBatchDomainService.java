package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import java.util.List;

public interface StockFundFlowCaptureBatchDomainService {

    List<StockFundFlowCaptureBatchBO> list();

    StockFundFlowCaptureBatchBO findById(Long id);

    Long create(StockFundFlowCaptureBatchBO value);

    boolean update(StockFundFlowCaptureBatchBO value);

    boolean delete(Long id);
}
