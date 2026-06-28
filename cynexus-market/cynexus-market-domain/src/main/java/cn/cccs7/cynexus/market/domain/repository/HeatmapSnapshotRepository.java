package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import java.util.List;

public interface HeatmapSnapshotRepository {

    List<HeatmapSnapshotBO> list();

    HeatmapSnapshotBO findById(Long id);

    Long create(HeatmapSnapshotBO value);

    boolean update(HeatmapSnapshotBO value);

    boolean delete(Long id);
}
