package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import java.util.List;
import java.util.Optional;

public interface TdxSectorQuoteDetailSnapshotRepository {

    List<TdxSectorQuoteDetailSnapshotBO> list();

    Optional<TdxSectorQuoteDetailSnapshotBO> findLatestByCode6(String code6);

    TdxSectorQuoteDetailSnapshotBO findById(Long id);

    Long create(TdxSectorQuoteDetailSnapshotBO value);

    boolean update(TdxSectorQuoteDetailSnapshotBO value);

    boolean delete(Long id);
}
