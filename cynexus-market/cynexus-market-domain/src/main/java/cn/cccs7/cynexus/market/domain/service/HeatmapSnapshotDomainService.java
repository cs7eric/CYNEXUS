package cn.cccs7.cynexus.market.domain.service;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import java.util.List;

public interface HeatmapSnapshotDomainService {

    List<HeatmapSnapshotBO> list();

    HeatmapSnapshotBO findById(Long id);

    Long create(HeatmapSnapshotBO value);

    boolean update(HeatmapSnapshotBO value);

    boolean delete(Long id);
}
