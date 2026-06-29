package cn.cccs7.cynexus.ai.infra.basic.service;

import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.common.db.service.CrudBasicService;
import java.util.List;

public interface ApplicationAnalysisResultHistoryBasicService extends CrudBasicService<ApplicationAnalysisResultHistoryPO> {

    List<ApplicationAnalysisResultHistoryPO> listActiveByTargetKey(String targetKey, int limit);
}
