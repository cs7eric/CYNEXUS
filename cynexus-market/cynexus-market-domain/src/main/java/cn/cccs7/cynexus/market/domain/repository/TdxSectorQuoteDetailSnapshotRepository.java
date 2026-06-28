package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import java.util.List;

public interface TdxSectorQuoteDetailSnapshotRepository {

    List<TdxSectorQuoteDetailSnapshotBO> list();

    TdxSectorQuoteDetailSnapshotBO findById(Long id);

    Long create(TdxSectorQuoteDetailSnapshotBO value);

    boolean update(TdxSectorQuoteDetailSnapshotBO value);

    boolean delete(Long id);
}
