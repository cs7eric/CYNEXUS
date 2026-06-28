package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import java.util.List;

public interface StyleSectorSnapshotRepository {

    List<StyleSectorSnapshotBO> list();

    StyleSectorSnapshotBO findById(Long id);

    Long create(StyleSectorSnapshotBO value);

    boolean update(StyleSectorSnapshotBO value);

    boolean delete(Long id);
}
