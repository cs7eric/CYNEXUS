package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import java.util.List;

public interface ApplicationAnalysisDailySnapshotRepository {

    List<ApplicationAnalysisDailySnapshotBO> listAll();

    ApplicationAnalysisDailySnapshotBO findById(Long id);

    Long create(ApplicationAnalysisDailySnapshotBO snapshot);

    boolean update(ApplicationAnalysisDailySnapshotBO snapshot);

    boolean delete(Long id);
}
