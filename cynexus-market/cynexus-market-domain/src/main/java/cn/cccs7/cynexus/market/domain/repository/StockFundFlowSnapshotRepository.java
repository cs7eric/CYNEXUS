package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import java.util.List;

public interface StockFundFlowSnapshotRepository {

    List<StockFundFlowSnapshotBO> list();

    StockFundFlowSnapshotBO findById(Long id);

    Long create(StockFundFlowSnapshotBO value);

    boolean update(StockFundFlowSnapshotBO value);

    boolean delete(Long id);
}
