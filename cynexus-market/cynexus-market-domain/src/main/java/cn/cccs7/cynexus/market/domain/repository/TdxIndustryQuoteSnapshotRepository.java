package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import java.util.List;

public interface TdxIndustryQuoteSnapshotRepository {

    List<TdxIndustryQuoteSnapshotBO> list();

    TdxIndustryQuoteSnapshotBO findById(Long id);

    Long create(TdxIndustryQuoteSnapshotBO value);

    boolean update(TdxIndustryQuoteSnapshotBO value);

    boolean delete(Long id);
}
