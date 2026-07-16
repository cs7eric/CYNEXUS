package cn.cccs7.cynexus.market.domain.sector;

import cn.cccs7.cynexus.market.domain.entity.TdxSectorQuoteDetailSnapshotBO;
import java.util.List;

public interface TdxSectorQuoteDetailSnapshotDomainService {

    List<TdxSectorQuoteDetailSnapshotBO> list();

    TdxSectorQuoteDetailSnapshotBO findById(Long id);

    Long create(TdxSectorQuoteDetailSnapshotBO value);

    boolean update(TdxSectorQuoteDetailSnapshotBO value);

    boolean delete(Long id);
}
