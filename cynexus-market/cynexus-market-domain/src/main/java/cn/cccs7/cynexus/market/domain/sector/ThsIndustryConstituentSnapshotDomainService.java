package cn.cccs7.cynexus.market.domain.sector;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import java.util.List;

public interface ThsIndustryConstituentSnapshotDomainService {

    List<ThsIndustryConstituentSnapshotBO> list();

    ThsIndustryConstituentSnapshotBO findById(Long id);

    Long create(ThsIndustryConstituentSnapshotBO value);

    boolean update(ThsIndustryConstituentSnapshotBO value);

    boolean delete(Long id);
}
