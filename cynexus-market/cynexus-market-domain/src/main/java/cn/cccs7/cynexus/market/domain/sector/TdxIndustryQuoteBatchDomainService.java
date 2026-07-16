package cn.cccs7.cynexus.market.domain.sector;

import cn.cccs7.cynexus.market.domain.entity.TdxIndustryQuoteBatchBO;
import java.util.List;

public interface TdxIndustryQuoteBatchDomainService {

    List<TdxIndustryQuoteBatchBO> list();

    TdxIndustryQuoteBatchBO findById(Long id);

    Long create(TdxIndustryQuoteBatchBO value);

    boolean update(TdxIndustryQuoteBatchBO value);

    boolean delete(Long id);
}
