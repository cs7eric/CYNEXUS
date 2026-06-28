package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteSnapshotBO;
import java.util.List;

public interface TdxIndustryQuoteSnapshotDomainService {

    List<TdxIndustryQuoteSnapshotBO> list();

    TdxIndustryQuoteSnapshotBO findById(Long id);

    Long create(TdxIndustryQuoteSnapshotBO value);

    boolean update(TdxIndustryQuoteSnapshotBO value);

    boolean delete(Long id);
}
