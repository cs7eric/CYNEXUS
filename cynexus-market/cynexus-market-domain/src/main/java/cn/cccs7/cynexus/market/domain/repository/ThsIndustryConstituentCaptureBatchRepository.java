package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentCaptureBatchBO;
import java.util.List;

public interface ThsIndustryConstituentCaptureBatchRepository {

    List<ThsIndustryConstituentCaptureBatchBO> list();

    ThsIndustryConstituentCaptureBatchBO findById(Long id);

    Long create(ThsIndustryConstituentCaptureBatchBO value);

    boolean update(ThsIndustryConstituentCaptureBatchBO value);

    boolean delete(Long id);
}
