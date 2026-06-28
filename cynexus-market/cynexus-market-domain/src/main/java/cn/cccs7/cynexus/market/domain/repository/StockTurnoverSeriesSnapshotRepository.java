package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import java.util.List;

public interface StockTurnoverSeriesSnapshotRepository {

    List<StockTurnoverSeriesSnapshotBO> list();

    StockTurnoverSeriesSnapshotBO findById(Long id);

    Long create(StockTurnoverSeriesSnapshotBO value);

    boolean update(StockTurnoverSeriesSnapshotBO value);

    boolean delete(Long id);
}
