package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import java.util.List;

public interface TdxIndustryQuoteBatchRepository {

    List<TdxIndustryQuoteBatchBO> list();

    TdxIndustryQuoteBatchBO findById(Long id);

    Long create(TdxIndustryQuoteBatchBO value);

    boolean update(TdxIndustryQuoteBatchBO value);

    boolean delete(Long id);
}
