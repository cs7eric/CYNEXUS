package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import java.util.List;

public interface ThsIndustryConstituentCaptureBatchDomainService {

    List<ThsIndustryConstituentCaptureBatchBO> list();

    ThsIndustryConstituentCaptureBatchBO findById(Long id);

    Long create(ThsIndustryConstituentCaptureBatchBO value);

    boolean update(ThsIndustryConstituentCaptureBatchBO value);

    boolean delete(Long id);
}
