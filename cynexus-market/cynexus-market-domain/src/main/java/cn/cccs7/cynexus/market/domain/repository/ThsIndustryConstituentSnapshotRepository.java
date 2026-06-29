package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import java.util.List;

public interface ThsIndustryConstituentSnapshotRepository {

    List<ThsIndustryConstituentSnapshotBO> list();

    List<ThsIndustryConstituentSnapshotBO> listLatestByIndustryCode(String industryCode);

    ThsIndustryConstituentSnapshotBO findById(Long id);

    Long create(ThsIndustryConstituentSnapshotBO value);

    boolean update(ThsIndustryConstituentSnapshotBO value);

    boolean delete(Long id);
}
