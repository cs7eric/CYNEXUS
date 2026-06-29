package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import java.time.LocalDate;
import java.util.List;

public interface ApplicationAnalysisDailySnapshotBasicService extends CrudBasicService<ApplicationAnalysisDailySnapshotPO> {

    List<ApplicationAnalysisDailySnapshotPO> listRecent30ByTargetKey(String targetKey, int limit);

    ApplicationAnalysisDailySnapshotPO getRecent30ByTargetKeyAndDate(String targetKey, LocalDate tradeDate);
}
