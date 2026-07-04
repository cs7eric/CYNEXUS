package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TdxSectorQuoteDetailSnapshotRepository {

    List<TdxSectorQuoteDetailSnapshotBO> list();

    Optional<TdxSectorQuoteDetailSnapshotBO> findLatestByCode6(String code6);

    Optional<TdxSectorQuoteDetailSnapshotBO> findLatestByCode6OnOrBefore(String code6, LocalDate snapshotDate);

    TdxSectorQuoteDetailSnapshotBO findById(Long id);

    Long create(TdxSectorQuoteDetailSnapshotBO value);

    boolean update(TdxSectorQuoteDetailSnapshotBO value);

    boolean delete(Long id);
}
