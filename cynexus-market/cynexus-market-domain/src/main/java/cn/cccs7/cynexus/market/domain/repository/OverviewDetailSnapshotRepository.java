package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import java.util.List;

public interface OverviewDetailSnapshotRepository {

    List<OverviewDetailSnapshotBO> list();

    OverviewDetailSnapshotBO findById(Long id);

    Long create(OverviewDetailSnapshotBO value);

    boolean update(OverviewDetailSnapshotBO value);

    boolean delete(Long id);
}
